package com.example.reverseengineeringbuildphase.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.reverseengineeringbuildphase.BuildConfig
import java.io.File
import java.security.MessageDigest

enum class DeviceRisk {
    Low,
    Medium,
    High
}

/**
 * Lightweight runtime checks: debuggable release, debugger connected, installer sanity.
 * Intended to **inform** UX (e.g. limit sensitive actions) rather than hard-crash.
 */
object IntegrityChecker {

    fun assess(context: Context): DeviceRisk {
        val pm = context.packageManager
        val appInfo = context.applicationInfo
        val debuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (BuildConfig.DEBUG) {
            return DeviceRisk.Low
        }

        if (debuggable) {
            return DeviceRisk.High
        }

        if (android.os.Debug.isDebuggerConnected()) {
            return DeviceRisk.High
        }

        if (hasHookFrameworkLoaded() || hasSuspiciousMapsEntry()) {
            return DeviceRisk.High
        }

        if (hasRootArtifacts() || hasTestKeysBuild()) {
            return DeviceRisk.Medium
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val installer = runCatching { pm.getInstallSourceInfo(context.packageName).installingPackageName }.getOrNull()
            if (installer != null && installer != "com.android.vending" && installer != "com.google.android.packageinstaller") {
                return DeviceRisk.Medium
            }
        }

        return DeviceRisk.Low
    }

    private fun hasHookFrameworkLoaded(): Boolean {
        val suspiciousClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook",
            "com.saurik.substrate.MS",
            "com.swift.sandhook.SandHook"
        )
        return suspiciousClasses.any { className ->
            runCatching { Class.forName(className, false, javaClass.classLoader) }.isSuccess
        }
    }

    private fun hasSuspiciousMapsEntry(): Boolean {
        val markers = listOf("frida", "xposed", "lsposed", "substrate", "riru", "zygisk")
        return runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line -> markers.any { marker -> line.contains(marker, ignoreCase = true) } }
            }
        }.getOrDefault(false)
    }

    private fun hasRootArtifacts(): Boolean {
        val suspiciousPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/bin/.ext/.su",
            "/cache/su",
            "/data/adb/magisk",
            "/sbin/.magisk",
            "/init.magisk.rc"
        )
        return suspiciousPaths.any { path -> File(path).exists() }
    }

    private fun hasTestKeysBuild(): Boolean {
        return Build.TAGS?.contains("test-keys", ignoreCase = true) == true
    }

    /**
     * Optional release signing certificate SHA-256 (hex, no colons) from [BuildConfig.EXPECTED_SIGNING_CERT_SHA256].
     * Empty → check skipped.
     */
    fun signingCertMatchesExpected(context: Context): Boolean {
        val expected = BuildConfig.EXPECTED_SIGNING_CERT_SHA256.trim().lowercase()
        if (expected.isEmpty()) return true
        val actual = signingCertSha256Hex(context) ?: return false
        return actual == expected
    }

    private fun signingCertSha256Hex(context: Context): String? {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val certDer: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                val signing = info.signingInfo ?: return null
                val der = if (signing.hasMultipleSigners()) {
                    signing.apkContentsSigners.firstOrNull()?.toByteArray()
                } else {
                    signing.signingCertificateHistory.firstOrNull()?.toByteArray()
                }
                der ?: return null
            } else {
                @Suppress("DEPRECATION")
                val sigs = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
                    ?: return null
                sigs.firstOrNull()?.toByteArray() ?: return null
            }
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(certDer).joinToString("") { b -> "%02x".format(b) }
        } catch (_: Exception) {
            null
        }
    }
}
