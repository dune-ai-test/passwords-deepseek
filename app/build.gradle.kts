plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aegis.vault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aegis.vault"
        minSdk = 26
        targetSdk = 35
        versionCode = 130
        versionName = "4.3.0"
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

    signingConfigs {
        // The debug key is committed to this repo on purpose: a public,
        // throwaway key (same spirit as the AOSP debug keystore) with the
        // classic "android" passwords. A fixed key means every CI build is
        // signed identically, so new APKs update in place over old installs
        // instead of failing with INSTALL_FAILED_UPDATE_INCOMPATIBLE.
        getByName("debug") {
            storeFile = rootProject.file("keystore/aegis-debug.p12")
            storeType = "PKCS12"
            storePassword = "android"
            keyAlias = "aegisdebug"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.biometric)
    // Pin a modern fragment: the old fragment pulled in transitively by
    // androidx.biometric 1.1.0 rejects the high activity-result request codes
    // used by activity 1.9.x ("Can only use lower 16 bits for requestCode"),
    // which crashed every file-picker / document-create launch.
    implementation(libs.androidx.fragment)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
