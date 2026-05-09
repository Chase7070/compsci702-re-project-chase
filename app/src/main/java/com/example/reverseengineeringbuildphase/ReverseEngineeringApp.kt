package com.example.reverseengineeringbuildphase

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.reverseengineeringbuildphase.data.network.NetworkModule

class ReverseEngineeringApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.okHttpClient)
            .build()
    }
}
