plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lifedesk.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lifedesk.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0.0"
        vectorDrawables { useSupportLibrary = true }

        // Firebase (accounts, password reset emails, cloud backup). Supplied at build time, e.g. from GitHub secrets.
        // When empty the app runs in offline mode without accounts.
        fun env(name: String) = "\"${System.getenv(name) ?: ""}\""
        buildConfigField("String", "FIREBASE_API_KEY", env("FIREBASE_API_KEY"))
        buildConfigField("String", "FIREBASE_APP_ID", env("FIREBASE_APP_ID"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", env("FIREBASE_PROJECT_ID"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", env("GOOGLE_WEB_CLIENT_ID"))
    }

    // Release signing is optional: set LIFEDESK_KEYSTORE / _PASSWORD / _ALIAS / _KEY_PASSWORD
    // (e.g. as CI secrets) to produce a signed release APK. Otherwise use the debug APK.
    val keystorePath = System.getenv("LIFEDESK_KEYSTORE")
    signingConfigs {
        // A fixed debug key (committed on purpose) so every CI build can update the previous install
        // and so Google Sign-In can be registered with one SHA-1. Use a private release key for the Play Store.
        getByName("debug") {
            storeFile = file("lifedesk-debug.keystore")
            storePassword = "android"
            keyAlias = "lifedesk"
            keyPassword = "android"
        }
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("LIFEDESK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LIFEDESK_KEY_ALIAS")
                keyPassword = System.getenv("LIFEDESK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.biometric:biometric:1.1.0")

    // Accounts (email/password + Google), password reset & verification emails, cloud backup
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // On-device OCR (bundled model: works offline, no Google Play Services download needed)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
