import org.gradle.api.GradleException
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Build-time AES-GCM encryption for release bootstrap material.
 * Plaintext is read only from local secrets.properties (never committed).
 */

fun Project.configureCredentialFragmentGeneration(
    secrets: Properties,
    secret: (String, String) -> String,
    isReleaseBuildRequested: Boolean
) {
    val credentialOutDir = layout.buildDirectory.dir("generated/credential-src/com/example/reverseengineeringbuildphase/security/credential/fragments")

    fun intArrayToBytes(values: IntArray): ByteArray {
        val out = ByteArray(values.size)
        for (i in values.indices) {
            out[i] = values[i].toByte()
        }
        return out
    }

    fun deriveAesKey(packageName: String, signingCertSha256Hex: String, versionCode: Int): ByteArray {
        val salts = listOf(
            intArrayOf(0x4e, 0x6f, 0x72, 0x74, 0x68, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x61),
            intArrayOf(0x45, 0x61, 0x73, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x62, 0x31),
            intArrayOf(0x53, 0x6f, 0x75, 0x74, 0x68, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x63),
            intArrayOf(0x57, 0x65, 0x73, 0x74, 0x5f, 0x73, 0x61, 0x6c, 0x74, 0x5f, 0x64, 0x32)
        )
        val md = MessageDigest.getInstance("SHA-256")
        md.update(packageName.toByteArray(Charsets.UTF_8))
        md.update(signingCertSha256Hex.trim().lowercase().toByteArray(Charsets.UTF_8))
        md.update(versionCode.toString().toByteArray(Charsets.UTF_8))
        for (salt in salts) {
            md.update(intArrayToBytes(salt))
        }
        return md.digest()
    }

    fun aesGcmEncrypt(plain: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    fun sha256HexFromDer(der: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(der).joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
    }

    fun resolveDebugKeystoreSha256(): String {
        val home = System.getProperty("user.home")
        val keystore = File(home, ".android/debug.keystore")
        if (!keystore.exists()) {
            throw GradleException("debug.keystore not found at ${keystore.absolutePath}")
        }
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        keystore.inputStream().use { stream ->
            ks.load(stream, "android".toCharArray())
        }
        val cert = ks.getCertificate("androiddebugkey") ?: throw GradleException("androiddebugkey missing")
        return sha256HexFromDer(cert.encoded)
    }

    fun resolveSigningSha256ForCodegen(): String {
        val configured = secret("RELEASE_SIGNING_SHA256", "").trim().lowercase()
        if (configured.isNotEmpty()) return configured
        return resolveDebugKeystoreSha256()
    }

    fun byteArrayLiteral(bytes: ByteArray): String {
        // Kotlin byteArrayOf() requires Byte; hex literals > 0x7f need an explicit cast.
        return bytes.joinToString(", ") { b -> "(0x%02x).toByte()".format(b.toInt() and 0xff) }
    }

    fun writeFragment(className: String, bytes: ByteArray, outDir: File) {
        outDir.mkdirs()
        val file = File(outDir, "$className.kt")
        file.writeText(
            """
            package com.example.reverseengineeringbuildphase.security.credential.fragments

            /** Build-generated ciphertext segment; do not edit manually. */
            internal object $className {
                fun payload(): ByteArray = byteArrayOf(${byteArrayLiteral(bytes)})
            }
            """.trimIndent() + "\n"
        )
    }

    tasks.register("generateCredentialFragments") {
        group = "build setup"
        description = "Encrypt AUTH_HEADER_VALUE into release credential fragments (build dir only)."
        outputs.dir(credentialOutDir)

        doLast {
            val plain = secret("AUTH_HEADER_VALUE", "").trim()
            if (plain.isEmpty()) {
                if (isReleaseBuildRequested) {
                    throw GradleException(
                        "Release builds require AUTH_HEADER_VALUE in secrets.properties " +
                            "to generate encrypted credential fragments."
                    )
                }
                val outDir = credentialOutDir.get().asFile
                listOf("MaterialSegmentAlpha", "MaterialSegmentBeta", "MaterialSegmentGamma", "MaterialSegmentDelta")
                    .forEach { name -> writeFragment(name, ByteArray(0), outDir) }
                return@doLast
            }

            val packageName = "com.example.reverseengineeringbuildphase"
            val versionCode = 1
            val signingHex = resolveSigningSha256ForCodegen()
            val key = deriveAesKey(packageName, signingHex, versionCode)
            val combined = aesGcmEncrypt(plain.toByteArray(Charsets.UTF_8), key)

            val chunkSizes = listOf(
                combined.size / 4,
                combined.size / 4,
                combined.size / 4,
                combined.size - 3 * (combined.size / 4)
            )
            var offset = 0
            val names = listOf("MaterialSegmentAlpha", "MaterialSegmentBeta", "MaterialSegmentGamma", "MaterialSegmentDelta")
            val outDir = credentialOutDir.get().asFile
            outDir.mkdirs()
            names.zip(chunkSizes).forEach { (name, size) ->
                val chunk = combined.copyOfRange(offset, offset + size)
                writeFragment(name, chunk, outDir)
                offset += size
            }
        }
    }
}

// Self-configure when applied from app/build.gradle.kts (cannot call across script compile units).
val credentialSecrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

fun credentialSecret(name: String, defaultValue: String = ""): String =
    credentialSecrets.getProperty(name, defaultValue)

val credentialIsReleaseBuildRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("release", ignoreCase = true)
}

configureCredentialFragmentGeneration(
    credentialSecrets,
    ::credentialSecret,
    credentialIsReleaseBuildRequested
)
