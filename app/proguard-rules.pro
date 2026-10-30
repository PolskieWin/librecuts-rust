# Keep native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.librecuts.rust.engine.** { *; }
-keep class * implements kotlinx.serialization.KSerializer
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

