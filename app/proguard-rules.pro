# Lista Fácil — ProGuard/R8 (release)

# Serialização (DTOs do contrato JSON com o backend)
-keepattributes *Annotation*, Signature, Exception, InnerClasses
-keepclassmembers class com.listafacil.app.data.remote.dto.** {
    <init>(...);
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Retrofit / OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# DataStore / Preferences
-dontwarn androidx.datastore.**
