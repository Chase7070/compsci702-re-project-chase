package com.example.reverseengineeringbuildphase.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.reverseengineeringbuildphase.BuildConfig
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

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

    private const val LOG_TAG = "REBP-Signing"

    private data class SigningDigestResult(
        val sha256Hex: String?,
        val source: String,
        val error: String? = null
    )

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
     * Release builds require a non-empty [BuildConfig.EXPECTED_SIGNING_CERT_SHA256] populated at build time.
     * Debug builds skip this check for local emulator work.
     */
    fun signingCertMatchesExpected(context: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        val expected = BuildConfig.EXPECTED_SIGNING_CERT_SHA256.trim().lowercase()
        val digest = resolveSigningDigest(context)
        val actual = digest.sha256Hex
        val match = expected.isNotEmpty() && actual != null && actual == expected
        if (!match) {
            logSigningMismatch(expected, digest, match)
        }
        return match
    }

    fun signingCertSha256Hex(context: Context): String? = resolveSigningDigest(context).sha256Hex

    private fun resolveSigningDigest(context: Context): SigningDigestResult {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                val signing = info.signingInfo
                    ?: return SigningDigestResult(null, "none", "signingInfo=null")

                val apkSigners = signing.apkContentsSigners
                if (!apkSigners.isNullOrEmpty()) {
                    val signatureBytes = apkSigners[0].toByteArray()
                    return SigningDigestResult(
                        sha256Hex = x509Sha256Hex(signatureBytes),
                        source = "apkContentsSigners[0]"
                    )
                }

                val history = signing.signingCertificateHistory
                if (!history.isNullOrEmpty()) {
                    val signatureBytes = history.last().toByteArray()
                    return SigningDigestResult(
                        sha256Hex = x509Sha256Hex(signatureBytes),
                        source = "signingCertificateHistory.last"
                    )
                }

                SigningDigestResult(null, "none", "no apkContentsSigners or signingCertificateHistory")
            } else {
                @Suppress("DEPRECATION")
                val sigs = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
                if (sigs.isNullOrEmpty()) {
                    return SigningDigestResult(null, "legacy.signatures", "signatures null or empty")
                }
                val signatureBytes = sigs[0].toByteArray()
                SigningDigestResult(
                    sha256Hex = x509Sha256Hex(signatureBytes),
                    source = "legacy.signatures[0]"
                )
            }
        } catch (e: Exception) {
            SigningDigestResult(
                sha256Hex = null,
                source = "error",
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun x509Sha256Hex(signatureBytes: ByteArray): String {
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(signatureBytes)) as X509Certificate
        return MessageDigest.getInstance("SHA-256")
            .digest(cert.encoded)
            .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
    }

    private fun logSigningMismatch(expected: String, digest: SigningDigestResult, match: Boolean) {
        val actual = digest.sha256Hex
        val errorSuffix = digest.error?.let { " error=$it" }.orEmpty()
        Log.w(
            LOG_TAG,
            "signing mismatch: expected.len=${expected.length} expected.pref=${expected.take(8)} " +
                "actual.len=${actual?.length ?: -1} actual.pref=${actual?.take(8)} " +
                "match=$match source=${digest.source}$errorSuffix"
        )
    }
}
