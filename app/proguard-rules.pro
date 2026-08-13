# kotlinx.serialization keeps generated serializers via @Serializable; the
# plugin emits the needed keep rules, but retain the annotations to be safe.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Retrofit interfaces are accessed reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
