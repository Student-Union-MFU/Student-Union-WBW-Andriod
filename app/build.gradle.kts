plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "th.ac.mfu.su.wbw"
    // 37 because `backdrop` refuses to be consumed by anything compiling against
    // less. Only compileSdk moved — targetSdk stays at 36, since raising that opts
    // the app into new runtime behaviour and is a separate decision from being able
    // to see the newer APIs at compile time.
    compileSdk = 37

    defaultConfig {
        applicationId = "th.ac.mfu.su.wbw"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            // Emulator loopback to the host machine's :8080 (Go backend). On a
            // physical device replace with the LAN IP or tunnel host + /wbw/.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/wbw/\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // The live SUS backend — same host the shipping iOS build points at
            // (`Backend.susProd` in wbw-ios-fontend/WBW/Config.swift). The trailing
            // slash matters: Retrofit resolves the @GET paths relative to it, and
            // without it the last segment is replaced instead of appended.
            buildConfigField("String", "API_BASE_URL", "\"https://api.studentunion.social/wbw/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.sceneview)
    implementation(libs.backdrop)
}
