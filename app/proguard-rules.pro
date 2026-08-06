# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Kotlinx Serialization
# Official rules from the kotlinx.serialization README. R8 fully supports these.
# Without them, R8 strips the generated $$serializer classes and Companion
# serializer() methods, and Json.decodeFromString<...>() throws
# "SerializationException: Serializer not found".
# ---------------------------------------------------------------------------

# Keep annotations used at runtime for (polymorphic) serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the generated serializers for this app's @Serializable classes (DTOs, push payloads).
-keep,includedescriptorclasses class com.pfitzer.tootsie.**$$serializer { *; }

# ---------------------------------------------------------------------------
# Retrofit
# Retrofit ships its own consumer R8 rules, but keep the generic signatures and
# checked exceptions so parameterized return types (List<StatusDto>, Response<X>)
# survive shrinking and the kotlinx-serialization converter can resolve them.
# ---------------------------------------------------------------------------
-keepattributes Signature, Exceptions, InnerClasses

# ---------------------------------------------------------------------------
# Room
# Room ships consumer rules; these are defensive keeps for the database and its
# paging integration.
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**