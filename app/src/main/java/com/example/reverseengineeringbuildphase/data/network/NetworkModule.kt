package com.example.reverseengineeringbuildphase.data.network

import android.content.Context
import com.example.reverseengineeringbuildphase.BuildConfig
import com.example.reverseengineeringbuildphase.data.repository.SessionRepository
import com.example.reverseengineeringbuildphase.data.security.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val ASSET_AUTH_BOOTSTRAP = "auth_bootstrap.txt"

    private lateinit var appContext: Context
    private lateinit var tokenManager: TokenManager
    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var apiService: ApiService
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    fun install(context: Context) {
        if (::tokenManager.isInitialized) return
        val app = context.applicationContext
        appContext = app
        tokenManager = TokenManager(app)
        val assetReader: () -> String? = {
            if (!BuildConfig.DEBUG) {
                null
            } else {
                try {
                    app.assets.open(ASSET_AUTH_BOOTSTRAP).bufferedReader().use { it.readText() }
                } catch (_: Exception) {
                    null
                }
            }
        }
        val localBootstrapReader: () -> String? = {
            BuildConfig.LOCAL_BOOTSTRAP_AUTH_VALUE
                .trim()
                .takeIf { value -> value.isNotEmpty() }
        }
        val pinner = PinningConfigurator.certificatePinnerOrNull()

        val publicClient = newOkHttpClientBuilder(pinner, withAuthorization = false).build()
        val publicRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(publicClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val publicApi = publicRetrofit.create(ApiService::class.java)
        sessionRepository = SessionRepository(publicApi, tokenManager, assetReader, localBootstrapReader)

        okHttpClient = newOkHttpClientBuilder(pinner, withAuthorization = true)
            .authenticator(TokenRefreshAuthenticator(tokenManager, sessionRepository))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun newOkHttpClientBuilder(
        certificatePinner: okhttp3.CertificatePinner?,
        withAuthorization: Boolean
    ): OkHttpClient.Builder {
        return OkHttpClient.Builder().apply {
            if (certificatePinner != null) {
                certificatePinner(certificatePinner)
            }
            addInterceptor(SensitiveRequestGuardInterceptor(appContext))
            addInterceptor(HostAllowlistInterceptor(AllowedHosts.asSet()))
            if (withAuthorization) {
                addInterceptor(AuthorizationInterceptor { tokenManager.getAuthorizationHeaderValue() })
            }
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
            addInterceptor(ImageGetContentTypeInterceptor())
            connectTimeout(20, TimeUnit.SECONDS)
            readTimeout(45, TimeUnit.SECONDS)
            writeTimeout(45, TimeUnit.SECONDS)
            callTimeout(90, TimeUnit.SECONDS)
        }
    }
}
