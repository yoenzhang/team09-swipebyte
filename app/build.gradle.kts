plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.SwipeByte"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.SwipeByte"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // ✅ Upgraded to Java 17 for performance
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17" // ✅ Ensures compatibility with latest Kotlin features
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // ✅ Ensure compatibility with Compose 1.5+
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose BOM (Bill of Materials)
    implementation(platform(libs.androidx.compose.bom))

    // Core UI Components
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // ✅ Explicit Material3 dependency

    // Navigation for Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.7.5") // ✅ Navigation support

    // Image Loading (Coil for Jetpack Compose)
    implementation("io.coil-kt:coil-compose:2.2.2") // ✅ Required for image loading in HomeView

    // Hilt (if using dependency injection)
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0") // ✅ Optional but recommended

    // Testing dependencies
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.compose.material3:material3:1.2.0")
}