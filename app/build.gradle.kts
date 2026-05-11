plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("stringfog") version "5.2.0"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // Required so StringFog can Class.forName(StringFogImpl) during configuration (see upstream README).
        classpath("com.github.megatronking.stringfog:xor:5.0.0")
    }
}

import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import java.util.Properties

val secrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

fun secret(name: String, defaultValue: String = ""): String {
    return secrets.getProperty(name, defaultValue)
}

android {
    namespace = "com.example.reverseengineeringbuildphase"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.reverseengineeringbuildphase"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read local values from secrets.properties.
        // AUTHORIZATION_TOKEN should be the raw token only, with no "Bearer " prefix.
        buildConfigField("String", "BASE_URL", "\"${secret("API_BASE_URL", "https://ai.elliottwen.info/")}\"")
        buildConfigField("String", "AUTHORIZATION_TOKEN", "\"${secret("AUTH_HEADER_VALUE")}\"")
        // Optional comma-separated extra hosts for image/CDN (no scheme). BASE_URL host is always allowed.
        buildConfigField(
            "String",
            "ALLOWED_NETWORK_HOSTS",
            "\"${secret("ALLOWED_NETWORK_HOSTS", "").replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

configure<StringFogExtension> {
    // XOR runtime (must match implementation class below).
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    debug = false
    // Only our application code; do not fog third-party / generated deps.
    fogPackages = arrayOf("com.example.reverseengineeringbuildphase")
    kg = RandomKeyGenerator()
    mode = StringFogMode.bytes
    // Manifest has no package=; explicit id matches namespace / applicationId.
    packageName = "com.example.reverseengineeringbuildphase"
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("io.coil-kt:coil:2.7.0")

    // StringFog runtime decryptor (see stringfog { implementation = ... }).
    implementation("com.github.megatronking.stringfog:xor:5.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}