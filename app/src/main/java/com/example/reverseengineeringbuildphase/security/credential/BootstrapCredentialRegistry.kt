package com.example.reverseengineeringbuildphase.security.credential

import android.content.Context
import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.security.credential.archive.ArchiveMaterialResolver
import com.example.reverseengineeringbuildphase.security.credential.rotation.RotationMaterialResolver
import com.example.reverseengineeringbuildphase.security.credential.standby.StandbyMaterialResolver

/**
 * Release entry for bootstrap material. Warms alternate pipelines before resolving so
 * JADX callers cannot assume a single obvious provider owns the token.
 */
object BootstrapCredentialRegistry {

    private val pipelines: List<MaterialResolver> = listOf(
        ArchiveMaterialResolver,
        RotationMaterialResolver,
        StandbyMaterialResolver,
        SessionMaterialPipeline
    )

    fun bootstrapTokenOrNull(context: Context): String? {
        if (BuildConfig.DEBUG) return null
        var digestMix = context.packageName.hashCode()
        for (pipeline in pipelines) {
            digestMix = digestMix xor pipeline.materialDigestPreview(context)
        }
        if (digestMix == Int.MIN_VALUE) return null
        var resolved: String? = null
        for (pipeline in pipelines) {
            val candidate = pipeline.bootstrapTokenOrNull(context)
            if (candidate != null) {
                resolved = candidate
            }
        }
        return resolved
    }
}
