package com.example.reverseengineeringbuildphase.security.credential

import android.content.Context

/**
 * Legacy entry retained for call sites; delegates to [BootstrapCredentialRegistry].
 */
object RuntimeCredentialProvider {

    fun bootstrapTokenOrNull(context: Context): String? =
        BootstrapCredentialRegistry.bootstrapTokenOrNull(context)
}
