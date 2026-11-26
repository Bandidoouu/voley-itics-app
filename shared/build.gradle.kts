plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    androidLibrary {
        namespace = "mx.uanl.fime.voley.shared"
        compileSdk = 37
        minSdk = 26
        withHostTestBuilder {}
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
            implementation("io.ktor:ktor-client-core:3.3.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
            implementation("io.ktor:ktor-client-okhttp:3.3.3")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
