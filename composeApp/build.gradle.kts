plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()
    
    androidLibrary {
        namespace = "me.tomasan7.jecnamobile.composeapp"
        compileSdk = 36
        minSdk = 26
    }
    
    sourceSets {
        commonMain {
            dependencies {
                api(libs.jecnaAPI)
                
                implementation(libs.compose.material3.mp)
                implementation(libs.compose.material.icons.extended.mp)
                implementation(libs.compose.resources)
                implementation(libs.kotlinxDatetime)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinxCoroutines.swing)
        }
    }
}
