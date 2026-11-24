package com.example.reverseengineeringbuildphase.security.credential

import android.content.Context

/**
 * Pluggable bootstrap material pipelines. Multiple implementations exist so static
 * analysis cannot assume a single resolver type owns release credentials.
 */
internal interface MaterialResolver {

    fun bootstrapTokenOrNull(context: Context): String?

    /** Lightweight touch of segment bytes; result is mixed into registry warm-up only. */
    fun materialDigestPreview(context: Context): Int
}
