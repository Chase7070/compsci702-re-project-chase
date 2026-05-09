package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Hosts permitted for outbound HTTPS (API + image loads).
 * Always includes the host of [BuildConfig.BASE_URL]; optional extras from
 * [BuildConfig.ALLOWED_NETWORK_HOSTS] (comma-separated, no scheme).
 */
object AllowedHosts {
    private val hosts: Set<String> by lazy { computeHosts() }

    fun asSet(): Set<String> = hosts

    private fun computeHosts(): Set<String> {
        val out = LinkedHashSet<String>()
        BuildConfig.BASE_URL.trim().toHttpUrlOrNull()?.host?.lowercase()?.let(out::add)
        BuildConfig.ALLOWED_NETWORK_HOSTS
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .forEach(out::add)
        check(out.isNotEmpty()) {
            "AllowedHosts: could not derive any host from BASE_URL; check API_BASE_URL in secrets.properties."
        }
        return out
    }
}
