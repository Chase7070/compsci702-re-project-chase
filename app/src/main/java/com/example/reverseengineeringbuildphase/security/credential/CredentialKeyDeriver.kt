package com.example.reverseengineeringbuildphase.security.credential

import com.example.reverseengineeringbuildphase.BuildConfig
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Derives an AES-256 key from app identity, signing certificate, version, and scattered salts.
 * Re-signing the APK changes the certificate input and yields a different key.
 */
internal object CredentialKeyDeriver {

    fun deriveAesKey(packageName: String, signingCertSha256Hex: String, versionCode: Int): SecretKey {
        val normalizedCert = signingCertSha256Hex.trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(packageName.toByteArray(Charsets.UTF_8))
        digest.update(normalizedCert.toByteArray(Charsets.UTF_8))
        digest.update(versionCode.toString().toByteArray(Charsets.UTF_8))
        CredentialSaltMaterial.forEachSalt { digest.update(it) }
        val keyBytes = digest.digest()
        return SecretKeySpec(keyBytes, "AES")
    }

    fun expectedSigningCertHex(): String =
        BuildConfig.EXPECTED_SIGNING_CERT_SHA256.trim().lowercase()
}
