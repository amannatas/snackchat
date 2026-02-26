plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.snakchatai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.snakchatai"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // WebRTC Crash Fix
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs {
            pickFirsts.add("**/libjingle_peerconnection_so.so")
        }
        resources {
            excludes.add("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.okhttp)

    // GSON (Missing waala wapas add kar diya)
    implementation(libs.gson)

    // Firebase Main
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)

    // OTP Play Integrity Fix
    implementation(libs.firebase.appcheck.playintegrity)

    // Firebase UI & Third Party
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation(libs.cloudinary.android)

    // WebRTC & Mesibo
    implementation(libs.mesibo.webrtc)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}