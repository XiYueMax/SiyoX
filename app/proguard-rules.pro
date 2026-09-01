
-dontwarn **
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keep class epic.verify.api.** { *; }
-keep class XiYue.SiyoX.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
