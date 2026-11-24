package com.example.reverseengineeringbuildphase.security.credential.archive

import android.content.Context
import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.security.IntegrityChecker
import com.example.reverseengineeringbuildphase.security.credential.CredentialCipher
import com.example.reverseengineeringbuildphase.security.credential.MaterialResolver

internal object ArchiveMaterialResolver : MaterialResolver {

    override fun materialDigestPreview(context: Context): Int {
        if (BuildConfig.DEBUG) return 0
        return runCatching {
            ArchiveFragmentAssembler.combinedPayload().fold(0) { acc, byte ->
                acc xor (byte.toInt() and 0xff)
            }
        }.getOrDefault(0)
    }

    override fun bootstrapTokenOrNull(context: Context): String? {
        if (BuildConfig.DEBUG) return null
        if (!IntegrityChecker.signingCertMatchesExpected(context)) return null
        val signingHex = IntegrityChecker.signingCertSha256Hex(context) ?: return null
        val expected = ArchiveKeyLattice.expectedSigningCertHex()
        if (expected.isNotEmpty() && signingHex != expected) return null
        return runCatching {
            val key = ArchiveKeyLattice.deriveAesKey(
                packageName = context.packageName,
                signingCertSha256Hex = signingHex,
                versionCode = BuildConfig.VERSION_CODE
            )
            val blob = ArchiveFragmentAssembler.combinedPayload()
            val plain = CredentialCipher.decrypt(blob, key)
            String(plain, Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
