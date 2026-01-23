plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    androidLibrary {
        namespace = "me.tomasan7.jecnamobile.shared"
        compileSdk = 36
        minSdk = 26
    }
    
    sourceSets {
        commonMain {
            dependencies {
            }
        }
    }
}
