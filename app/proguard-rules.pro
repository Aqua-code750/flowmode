# FlowMode ProGuard Rules

# General Android rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Keep Firebase and GMS classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep your data models (important for Firestore serialization)
-keep class com.example.flowmode.data.model.** { *; }

# Obfuscate everything else
-dontwarn com.example.flowmode.**
-keep class com.example.flowmode.ui.** { *; }
# Note: You can fine-tune this to obfuscate more UI logic if needed,
# but keeping UI classes often prevents layout inflation issues.
