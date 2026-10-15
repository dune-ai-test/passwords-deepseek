# Keep this file even though minify is off — handy later for release builds.
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class com.aegis.vault.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
