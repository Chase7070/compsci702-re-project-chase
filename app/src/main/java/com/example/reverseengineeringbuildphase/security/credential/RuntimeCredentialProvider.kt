package com.example.reverseengineeringbuildphase.security.credential

import android.content.Context
import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.security.IntegrityChecker

/**
 * Resolves the release bootstrap credential at runtime from build-time encrypted fragments.
 * Debug builds use [com.example.reverseengineeringbuildphase.data.network.NetworkModule] asset fallback instead.
 */
object RuntimeCredentialProvider {

    fun bootstrapTokenOrNull(context: Context): String? {
        if (BuildConfig.DEBUG) return null
        if (!IntegrityChecker.signingCertMatchesExpected(context)) return null
        val signingHex = IntegrityChecker.signingCertSha256Hex(context) ?: return null
        val expected = CredentialKeyDeriver.expectedSigningCertHex()
        if (expected.isNotEmpty() && signingHex != expected) return null
        return runCatching {
            val key = CredentialKeyDeriver.deriveAesKey(
                packageName = context.packageName,
                signingCertSha256Hex = signingHex,
                versionCode = BuildConfig.VERSION_CODE
            )
            val blob = CredentialFragmentAssembler.combinedPayload()
            val plain = CredentialCipher.decrypt(blob, key)
            String(plain, Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
