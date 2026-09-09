# ===================================================================
# ProGuard / R8 Rules for Gold & Silver Live Calc
# ===================================================================

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.runtime.RecomposeScopeImpl { *; }

# Room Database Entities & DAOs
-keepclassmembers class androidx.room.RoomDatabase {
    public *;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.goldsilver.livecalc.data.local.entities.** { *; }
-keep class com.goldsilver.livecalc.data.local.daos.** { *; }

# WorkManager Workers
-keep public class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep public class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.goldsilver.livecalc.background.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    *;
}

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Firebase Remote Config & Messaging
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# App Specific Data Models
-keep class com.goldsilver.livecalc.data.models.** { *; }
-keep class com.goldsilver.livecalc.ui.viewmodel.** { *; }
-keep class com.goldsilver.livecalc.ota.** { *; }
