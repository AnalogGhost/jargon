# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hackerapps.jargon.data.**$$serializer { *; }
-keepclassmembers class com.hackerapps.jargon.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.hackerapps.jargon.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
