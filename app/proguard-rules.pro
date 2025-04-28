# Add project-specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# Optimize aggressively to reduce APK size
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!method/removal/parameter
-optimizationpasses 5
-allowaccessmodification

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Jetpack Compose ---
# Keep Compose runtime and Material components
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# Keep Material Icons Extended classes but allow shrinking unused icons
-keep class androidx.compose.material.icons.** { *; }
-keepclassmembers class androidx.compose.material.icons.extended.** {
    public static final androidx.compose.ui.graphics.vector.ImageVector *;
}
# Allow R8 to remove unused icons
-dontwarn androidx.compose.material.icons.extended.**

# Keep Compose Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Keep Compose ViewModel
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-dontwarn androidx.lifecycle.**

# --- WebView (for YoutubePreview.kt) ---
# Keep WebView and related classes
-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebViewClient { *; }
-keep class android.webkit.WebChromeClient { *; }
-dontwarn android.webkit.**

# Keep JavaScript interfaces for WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }
-dontwarn dagger.hilt.**
-dontwarn androidx.hilt.**

# Keep Hilt-generated classes
-keepclasseswithmembers class * {
    @dagger.hilt.** *;
}

# --- Room ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# --- Retrofit ---
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- Gson (used with Retrofit) ---
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keepattributes *Annotation*
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# --- OkHttp ---
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# --- Coil ---
-keep class coil.** { *; }
-dontwarn coil.**
-keep class coil.annotation.** { *; }
-keep @coil.annotation.ExperimentalCoilApi class * { *; }

# --- Kotlinx Serialization ---
# Keep Serializable and Polymorphic annotations
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep Companion object fields of serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serializer() on companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep INSTANCE.serializer() of serializable objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- ExoPlayer ---
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- PrettyTime ---
-keep class org.ocpsoft.prettytime.** { *; }
-dontwarn org.ocpsoft.prettytime.**

# --- Commons Text ---
-keep class org.apache.commons.text.** { *; }
-dontwarn org.apache.commons.text.**

# --- Chucker ---
-keep class com.chuckerteam.chucker.** { *; }
-dontwarn com.chuckerteam.chucker.**

# --- General AndroidX ---
-keep class androidx.** { *; }
-dontwarn androidx.**

# Prevent R8 from removing unused classes in release builds
-keep class com.example.animeapp.** { *; }
-dontwarn com.example.animeapp.**