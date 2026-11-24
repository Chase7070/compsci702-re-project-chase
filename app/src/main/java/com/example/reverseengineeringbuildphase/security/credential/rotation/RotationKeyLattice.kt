package com.example.reverseengineeringbuildphase.security.credential.rotation

import com.example.reverseengineeringbuildphase.BuildConfig
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

internal object RotationKeyLattice {

    fun deriveAesKey(packageName: String, signingCertSha256Hex: String, versionCode: Int): SecretKey {
        val normalizedCert = signingCertSha256Hex.trim().lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(packageName.toByteArray(Charsets.UTF_8))
        digest.update(normalizedCert.toByteArray(Charsets.UTF_8))
        digest.update(versionCode.toString().toByteArray(Charsets.UTF_8))
        RotationSaltLattice.forEachSalt { digest.update(it) }
        return SecretKeySpec(digest.digest(), "AES")
    }

    fun expectedSigningCertHex(): String =
        BuildConfig.EXPECTED_SIGNING_CERT_SHA256.trim().lowercase()
}
