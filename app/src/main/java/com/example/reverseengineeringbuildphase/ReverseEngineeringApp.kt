package com.example.reverseengineeringbuildphase

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.reverseengineeringbuildphase.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ReverseEngineeringApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NetworkModule.install(this)
        runBlocking(Dispatchers.IO) {
            runCatching {
                NetworkModule.sessionRepository.bootstrapIfNeeded()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.okHttpClient)
            .build()
    }
}
