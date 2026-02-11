plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    // https://developer.android.com/build/migrate-to-built-in-kotlin#migration-steps-migrate-kotlin-kapt-plugin
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.hilt)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

android {
    compileSdk = 36
    namespace = "me.tomasan7.jecnamobile"

    defaultConfig {
        applicationId = "me.tomasan7.jecnamobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 38
        versionName = "2.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("compose-destinations.codeGenPackageName", "me.tomasan7.jecnamobile")
}

/* Allow references to generated code */
/*kapt {
    correctErrorTypes = true
}*/

dependencies {
    implementation(libs.jecnaSupl.client) {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation(libs.jecnaSupl.android)
    implementation(libs.jna) {
        artifact { 
            type = "aar"
        }
    }
    implementation(libs.jecnaAPI)

    // https://github.com/google/dagger/issues/4693#issuecomment-2823736143
    kapt("androidx.room:room-compiler-processing:2.8.4")

    implementation(platform(libs.compose.android.bom))
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.composeHtml)
    implementation(libs.composeCoil)
    implementation(libs.composeStateEvents)
    implementation(libs.accompanist.permissions)
    implementation(libs.navigation3.ui)
    implementation(libs.navigation3.runtime)

    implementation(libs.activity.ktx)

    kapt(libs.hilt.compiler)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
}
