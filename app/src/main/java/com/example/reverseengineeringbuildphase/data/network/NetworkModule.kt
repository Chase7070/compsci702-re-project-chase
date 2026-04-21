package com.example.reverseengineeringbuildphase.data.network

import com.example.reverseengineeringbuildphase.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(AuthorizationInterceptor { BuildConfig.AUTHORIZATION_TOKEN })
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        connectTimeout(20, TimeUnit.SECONDS)
        readTimeout(45, TimeUnit.SECONDS)
        writeTimeout(45, TimeUnit.SECONDS)
        callTimeout(90, TimeUnit.SECONDS)
    }.build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
