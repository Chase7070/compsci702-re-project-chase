package com.example.reverseengineeringbuildphase.util

import android.content.res.Resources
import com.example.reverseengineeringbuildphase.R
import com.example.reverseengineeringbuildphase.data.network.SensitiveRequestBlockedException
import okhttp3.internal.http2.StreamResetException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

object NetworkErrorMapper {

    fun messageWithPrefix(resources: Resources, cause: Throwable, prefix: String): String {
        val detail = messageForThrowable(resources, cause)
        val p = prefix.trim()
        return if (p.isEmpty()) detail else "$p $detail"
    }

    fun messageForThrowable(resources: Resources, throwable: Throwable): String {
        val hit = throwable.findFirstKnownCause()
        return when (hit) {
            is SocketTimeoutException -> resources.getString(R.string.network_timeout)
            is ConnectException -> resources.getString(R.string.network_connect_failed)
            is UnknownHostException -> resources.getString(R.string.network_unknown_host)
            is StreamResetException -> resources.getString(R.string.network_stream_reset)
            is SensitiveRequestBlockedException ->
                hit.message?.takeIf { it.isNotBlank() }
                    ?: resources.getString(R.string.network_sensitive_blocked_default)
            is SSLPeerUnverifiedException ->
                resources.getString(R.string.network_ssl_peer_unverified)
            is SSLException -> resources.getString(R.string.network_ssl_failed)
            is HttpException -> httpMessage(resources, hit)
            is IOException ->
                resources.getString(
                    R.string.network_io_failed,
                    hit.message ?: hit.javaClass.simpleName
                )
            else ->
                hit.message?.takeIf { it.isNotBlank() }
                    ?: resources.getString(R.string.network_unknown_error, hit.javaClass.simpleName)
        }
    }

    private fun Throwable.findFirstKnownCause(): Throwable {
        val chain = causeChain(this)
        return chain.firstOrNull { t ->
            t is SocketTimeoutException ||
                t is ConnectException ||
                t is UnknownHostException ||
                t is StreamResetException ||
                t is SensitiveRequestBlockedException ||
                t is SSLPeerUnverifiedException ||
                t is SSLException ||
                t is HttpException
        } ?: chain.last()
    }

    private fun causeChain(root: Throwable): List<Throwable> {
        val out = ArrayList<Throwable>()
        var t: Throwable? = root
        val seen = HashSet<Throwable>()
        while (t != null && seen.add(t)) {
            out.add(t)
            t = t.cause
        }
        return out
    }

    private fun httpMessage(resources: Resources, http: HttpException): String {
        val code = http.code()
        val snippet = try {
            http.response()?.errorBody()?.string()
                ?.trim()
                ?.take(120)
        } catch (_: Exception) {
            null
        }
        return if (snippet.isNullOrBlank()) {
            resources.getString(R.string.http_error_code_only, code)
        } else {
            resources.getString(R.string.http_error_with_body, code, snippet)
        }
    }
}
