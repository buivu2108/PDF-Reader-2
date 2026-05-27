# PdfBox-Android
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# AndroidPdfViewer / PDFium
-keep class com.shockwave.** { *; }
-keep class com.github.barteksc.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Coil image loading
-dontwarn coil.**
-keep class coil.** { *; }

# Google Play Services - AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Google ML Kit Document Scanner
-keep class com.google.android.gms.mlkit.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Kotlin Serialization (if used by navigation)
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Compose - keep runtime stability annotations
-dontwarn androidx.compose.**

# Keep app model classes for reflection
-keep class com.pdfapp.reader.domain.model.** { *; }
-keep class com.pdfapp.reader.data.local.entity.** { *; }
