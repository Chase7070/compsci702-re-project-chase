# StringFog: keep generated entry + XOR impl (R8 must not strip decrypt path).
-keep class com.example.reverseengineeringbuildphase.StringFog { *; }
-keep class com.github.megatronking.stringfog.xor.StringFogImpl { *; }
-keep class com.github.megatronking.stringfog.** { *; }

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
