plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace   = "com.example.notificationfetcher"
    compileSdk  = 35  // ✅ Android 15 — latest stable public SDK

    defaultConfig {
        applicationId = "com.example.notificationfetcher"
        minSdk        = 24   // ✅ Android 7.0 — covers ~95% of devices
        targetSdk     = 35   // ✅ Match compileSdk
        versionCode   = 1
        versionName   = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        // ── DEBUG — for development & DB Inspector ──
        debug {
            isMinifyEnabled = false
            isDebuggable    = true
            applicationIdSuffix = ".debug"  // Allows debug + release installed side-by-side
            versionNameSuffix   = "-debug"
        }

        // ── RELEASE — for distribution on other devices ──
        release {
            isMinifyEnabled   = true   // ✅ Obfuscation ON
            isShrinkResources = true   // ✅ Remove unused resources
            isDebuggable      = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose     = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // ── Lifecycle ──
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ── Compose ──
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))  // ✅ Use version catalog BOM
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ── Room Database ──
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── JSON Export ──
    implementation(libs.gson)

    // ── Security (EncryptedSharedPreferences) ──
    implementation(libs.androidx.security.crypto)

    // ── SQLCipher (DB encryption in release builds) ──
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}