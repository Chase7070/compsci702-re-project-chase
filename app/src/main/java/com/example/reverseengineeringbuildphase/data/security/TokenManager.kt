package com.example.reverseengineeringbuildphase.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.atomic.AtomicReference

/**
 * Persists API access / refresh material in [EncryptedSharedPreferences] (AES-GCM + Android Keystore).
 * Uses `security-crypto:1.0.0` APIs ([MasterKeys] + legacy [EncryptedSharedPreferences.create] overload).
 * Authorization header value is stored as the **raw** header value (no automatic `Bearer ` prefix).
 */
class TokenManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val memoryAccess = AtomicReference<String?>(null)

    init {
        memoryAccess.set(prefs.getString(KEY_ACCESS, null))
    }

    fun getAuthorizationHeaderValue(): String = memoryAccess.get()?.trim().orEmpty()

    fun hasAccessToken(): Boolean = getAuthorizationHeaderValue().isNotEmpty()

    fun setAccessTokenOnly(rawHeaderValue: String) {
        val v = rawHeaderValue.trim()
        prefs.edit().putString(KEY_ACCESS, v).apply()
        memoryAccess.set(v.ifEmpty { null })
    }

    fun setTokens(accessTokenHeaderValue: String, refreshToken: String?) {
        val a = accessTokenHeaderValue.trim()
        prefs.edit()
            .putString(KEY_ACCESS, a)
            .putString(KEY_REFRESH, refreshToken?.trim()?.takeIf { it.isNotEmpty() })
            .apply()
        memoryAccess.set(a.ifEmpty { null })
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun clearSession() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply()
        memoryAccess.set(null)
    }

    private companion object {
        const val PREFS_NAME = "secure_api_tokens"
        const val KEY_ACCESS = "authorization_header_value"
        const val KEY_REFRESH = "refresh_token"
    }
}
