plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "mx.uanl.fime.voley"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "mx.uanl.fime.voley"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("release") {
            storeFile = providers.gradleProperty("VOLEY_STORE_FILE").orNull?.let { file(it) }
            storePassword = providers.gradleProperty("VOLEY_STORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("VOLEY_KEY_ALIAS").orNull
            keyPassword = providers.gradleProperty("VOLEY_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
