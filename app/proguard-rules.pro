# --- Keep Play2PDF app entry points ---
-keep class com.adnanfoisal.play2pdf.Play2PdfApp { *; }
-keep class com.adnanfoisal.play2pdf.MainActivity { *; }

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class *
-keepnames class * extends androidx.lifecycle.ViewModel
-keepnames class * extends androidx.lifecycle.AndroidViewModel

# --- Retrofit / OkHttp / Moshi ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class * { @com.squareup.moshi.Json <fields>; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Kotlin coroutines ---
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# --- Domain models (DTOs are referenced by reflection via Moshi) ---
-keep class com.adnanfoisal.play2pdf.data.api.** { *; }
-keep class com.adnanfoisal.play2pdf.domain.model.** { *; }
