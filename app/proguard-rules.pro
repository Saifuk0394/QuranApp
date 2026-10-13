# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class com.quran.romanurdu.network.** { *; }
-keep class com.quran.romanurdu.data.model.** { *; }
-keep class com.quran.romanurdu.data.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
