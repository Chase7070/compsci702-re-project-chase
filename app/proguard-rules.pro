# StringFog: keep generated entry + XOR impl (R8 must not strip decrypt path).
-keep class com.example.reverseengineeringbuildphase.StringFog { *; }
-keep class com.github.megatronking.stringfog.xor.StringFogImpl { *; }
-keep class com.github.megatronking.stringfog.xor.StringFogImpl { *; }

# Release credential: keep decrypt entry points; allow fragment classes to be renamed.
-keep class com.example.reverseengineeringbuildphase.security.credential.RuntimeCredentialProvider { *; }
-keep class com.example.reverseengineeringbuildphase.security.credential.CredentialCipher { *; }
-keep class com.example.reverseengineeringbuildphase.security.credential.CredentialKeyDeriver { *; }
-keep class com.example.reverseengineeringbuildphase.security.credential.CredentialFragmentAssembler { *; }
-keepclassmembers class com.example.reverseengineeringbuildphase.security.credential.fragments.** {
    byte[] payload();
}

# Keep annotation/signature metadata required by Retrofit.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# Keep Retrofit HTTP method annotations on interface members.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

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
