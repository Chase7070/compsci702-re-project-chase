package com.example.reverseengineeringbuildphase.util

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

    fun messageWithPrefix(cause: Throwable, prefix: String): String {
        val detail = messageForThrowable(cause)
        val p = prefix.trim()
        return if (p.isEmpty()) detail else "$p $detail"
    }

    fun messageForThrowable(throwable: Throwable): String {
        val hit = throwable.findFirstKnownCause()
        return when (hit) {
            is SocketTimeoutException -> "连接或读取超时，请稍后重试。"
            is ConnectException -> "无法连接服务器，请检查网络。"
            is UnknownHostException -> "无法解析服务器地址，请检查网络或配置。"
            is StreamResetException -> "连接被中断，请重试。"
            is SensitiveRequestBlockedException -> hit.message ?: "安全策略已阻止敏感请求。"
            is SSLPeerUnverifiedException ->
                "无法验证服务器身份（证书校验未通过）。若正在使用抓包代理或自定义证书，请关闭后重试；也可能是证书已轮换，需更新应用配置。"
            is SSLException -> "安全连接失败，请检查网络或证书。"
            is HttpException -> httpMessage(hit)
            is IOException -> "网络异常：${hit.message ?: hit.javaClass.simpleName}"
            else -> hit.message?.takeIf { it.isNotBlank() }
                ?: "未知错误（${hit.javaClass.simpleName}）。"
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

    private fun httpMessage(http: HttpException): String {
        val code = http.code()
        val snippet = try {
            http.response()?.errorBody()?.string()
                ?.trim()
                ?.take(120)
        } catch (_: Exception) {
            null
        }
        val base = "HTTP $code"
        return if (snippet.isNullOrBlank()) base else "$base：$snippet"
    }
}
