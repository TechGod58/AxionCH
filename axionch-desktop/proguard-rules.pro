# Desktop runtime does not ship Android APIs or optional TLS providers.
# OkHttp references these classes reflectively; suppress warnings for release packaging.
-dontwarn android.**
-dontwarn dalvik.system.**

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-dontwarn okhttp3.internal.platform.Android*
-dontwarn okhttp3.internal.platform.android.**
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform*
-dontwarn okhttp3.internal.platform.ConscryptPlatform*
-dontwarn okhttp3.internal.platform.OpenJSSEPlatform*
