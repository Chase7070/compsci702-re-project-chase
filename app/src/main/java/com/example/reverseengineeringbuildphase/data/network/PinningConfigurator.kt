package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object PinningConfigurator {

    /**
     * Comma-separated entries. Supported forms:
     * - `sha256/AAAA=` -> applies to the host of [BuildConfig.BASE_URL]
     * - `cdn.example.com=sha256/BBBB=` -> applies to the specified host
     *
     * Debug builds may leave it empty for local proxy work; release must provide pins for every
     * host in [AllowedHosts].
     */
    fun certificatePinnerOrNull(): CertificatePinner? {
        val baseHost = BuildConfig.BASE_URL.trim().toHttpUrlOrNull()?.host?.lowercase() ?: return null
        val raw = BuildConfig.CERTIFICATE_PINS.trim()
        check(BuildConfig.DEBUG || raw.isNotEmpty()) {
            "Release build requires CERT_PINS for host $baseHost."
        }
        if (raw.isEmpty()) return null
        val pinMap = parsePinMap(baseHost, raw)
        check(pinMap.isNotEmpty()) {
            "CERT_PINS is present but no valid pins were parsed for host $baseHost."
        }
        if (!BuildConfig.DEBUG) {
            val uncoveredHosts = AllowedHosts.asSet().filter { it !in pinMap.keys }
            check(uncoveredHosts.isEmpty()) {
                "Release build requires pins for every allowed host. Missing: ${uncoveredHosts.joinToString(", ")}"
            }
        }
        val builder = CertificatePinner.Builder()
        for ((host, pins) in pinMap) {
            for (pin in pins) {
                builder.add(host, pin)
            }
        }
        return builder.build()
    }

    private fun parsePinMap(baseHost: String, raw: String): Map<String, List<String>> {
        val out = linkedMapOf<String, MutableList<String>>()
        raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { entry ->
                val host: String
                val pin: String
                if (entry.startsWith("sha256/")) {
                    host = baseHost
                    pin = entry
                } else {
                    val idx = entry.indexOf('=')
                    require(idx > 0 && idx < entry.lastIndex) {
                        "Host-specific pin must use host=sha256/... form: $entry"
                    }
                    host = entry.substring(0, idx).trim().lowercase()
                    pin = entry.substring(idx + 1).trim()
                    require(host.isNotEmpty()) {
                        "Pin host must not be empty: $entry"
                    }
                }
                require(pin.startsWith("sha256/")) {
                    "Unsupported pin format for host $host: $pin"
                }
                out.getOrPut(host) { mutableListOf() }.add(pin)
            }
        return out
    }
}
