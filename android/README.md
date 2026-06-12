# Omni Android App

This directory is reserved for the native Android application (Kotlin/Java).

## Initialization Instructions

To properly initialize the Android application with the correct Gradle wrappers and structural defaults, please follow these steps:

1. Open **Android Studio**.
2. Select **New Project**.
3. Choose **Empty Activity** (or Jetpack Compose Empty Activity).
4. Name the application **Omni**.
5. Set the package name to `com.omni.app` (or your preferred namespace).
6. Set the save location directly to this directory: `/android`.
7. Choose **Kotlin** as the language.
8. Set the Minimum SDK to API 35 (Android 15) or as required.
9. Click **Finish**.

This will generate the required Android boilerplate safely. 
After generation, we can implement the FCM Service and Compose UI for enrollment.
