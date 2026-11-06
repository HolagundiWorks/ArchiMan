# Room, Hilt and Compose keep rules come from library consumer ProGuard files.
# Add explicit keeps here only when R8 reports missing classes after a release smoke test:
# - @TypeConverter classes
# - @DatabaseView classes
# - Classes loaded via Class.forName in portal/backup paths
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
