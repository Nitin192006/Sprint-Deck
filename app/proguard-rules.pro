# Add project specific ProGuard rules here.
# For more details, see: http://developer.android.com/guide/developing/tools/proguard.html

## --- General Shrink and Optimization Settings ---
# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations of all levels
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

## --- Jetpack Compose ---
# Keep Compose and dynamic UI classes, attributes, components
-keep class androidx.compose.compiler.plugins.kotlin.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNodeElement {
    <init>(...);
}

## --- Kotlin Coroutines ---
# Preserve coroutines debug and internal structures
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public <init>();
}

## --- Room Database ---
# Preserve the Room runtime classes and mapping mechanisms
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.migration.bundle.**
-dontwarn androidx.room.RoomDatabase

## --- Firebase Common & Firestore ---
# Keep database & user data models from being obfuscated or stripped (needed for serializing/deserializing keys)
-keepclassmembers class com.example.data.User {
    <fields>;
    <methods>;
}
-keepclassmembers class com.example.data.Hackathon {
    <fields>;
    <methods>;
}

# General Firebase ProGuard configs
-keepattributes *Database*
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
