# MediaPipe uses JNI + protobuf reflection
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.poseit.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.poseit.app.**$$serializer { *; }
