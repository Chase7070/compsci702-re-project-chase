package com.example.reverseengineeringbuildphase.data.network

/**
 * Relative API paths for [ApiService] [@Url] calls.
 * Built from fragments so literals are not embedded in Retrofit annotations (StringFog can fog method bodies / call sites as configured).
 */
object EndpointProvider {
    fun authPath(): String = "a" + "u" + "t" + "h"

    fun generateImagePath(): String = "generate" + "_" + "image"

    fun sessionPath(): String = "s" + "e" + "s" + "s" + "i" + "o" + "n"

    fun sessionRefreshPath(): String = sessionPath() + "/" + "r" + "e" + "f" + "r" + "e" + "s" + "h"
}
