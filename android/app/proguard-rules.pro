# Keep Room entity classes
-keep class com.focusapp.data.db.entity.** { *; }

# Keep Hilt-generated components
-keep class dagger.hilt.** { *; }

# Keep Timber in release (remove log calls)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
