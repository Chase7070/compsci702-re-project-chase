# StringFog: keep generated entry + XOR impl (R8 must not strip decrypt path).
-keep class com.example.reverseengineeringbuildphase.StringFog { *; }
-keep class com.github.megatronking.stringfog.xor.StringFogImpl { *; }

# Release bootstrap: keep public registry entry only; allow R8 to rename credential internals.
-keep class com.example.reverseengineeringbuildphase.security.credential.BootstrapCredentialRegistry { *; }
-keepclassmembers class com.example.reverseengineeringbuildphase.security.credential.** {
    byte[] payload();
}

# Android components / entry points (required for launch and Compose).
-keep class com.example.reverseengineeringbuildphase.ReverseEngineeringApp { *; }
-keep class com.example.reverseengineeringbuildphase.MainActivity { *; }
-keep class com.example.reverseengineeringbuildphase.ui.** { *; }

# Integrity + network guard: keep types; names may be obfuscated.
-keep,allowobfuscation class com.example.reverseengineeringbuildphase.security.IntegrityChecker { *; }
-keep,allowobfuscation class com.example.reverseengineeringbuildphase.data.network.SensitiveRequestGuardInterceptor { *; }

# Keep annotation/signature metadata required by Retrofit.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepattributes *Annotation*

# Kotlin metadata (reflection / coroutines used by some libraries).
-keepattributes KotlinMetadata

# Keep Retrofit HTTP method annotations on interface members.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface com.example.reverseengineeringbuildphase.data.network.ApiService { *; }

# Keep Gson fields annotated with @SerializedName.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson + Kotlin data classes used by Retrofit: keep concrete model types so Gson does not
# deserialize JSON into LinkedTreeMap and trigger ClassCastException in release (e.g. /auth).
-keep class com.example.reverseengineeringbuildphase.data.model.** { *; }

# Gson generic TypeToken (used internally for parameterized types).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# OkHttp / Retrofit platform types.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
