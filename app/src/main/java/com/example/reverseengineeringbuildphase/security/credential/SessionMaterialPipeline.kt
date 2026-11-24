package com.example.reverseengineeringbuildphase.security.credential

import android.content.Context
import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.security.IntegrityChecker

/** Primary release bootstrap pipeline (build-generated AES-GCM segments). */
internal object SessionMaterialPipeline : MaterialResolver {

    override fun materialDigestPreview(context: Context): Int {
        if (BuildConfig.DEBUG) return 0
        return runCatching {
            CredentialFragmentAssembler.combinedPayload().fold(0) { acc, byte ->
                acc xor (byte.toInt() and 0xff)
            }
        }.getOrDefault(0)
    }

    override fun bootstrapTokenOrNull(context: Context): String? {
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
