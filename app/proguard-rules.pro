# JNI upcalls are invisible to R8. The pinned Sherpa AAR has an empty proguard.txt.
# Keep vendor binding/data/callback classes, not the whole application.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# LiteRT-LM JNI resolves constructors, content objects and callbacks by name.
-keep class com.google.ai.edge.litertlm.** { *; }
# Local OPUS translator uses LiteRT's TensorFlow-compatible JNI facade.
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }

# Vosk uses JNA direct mapping and callbacks, including reflected structure fields.
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * extends com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Callback { *; }
# JNA desktop-only optional integration is unavailable on Android.
-dontwarn java.awt.**

# Keep useful line numbers in the uploaded mapping, while application code shrinks.
-keepattributes SourceFile,LineNumberTable
