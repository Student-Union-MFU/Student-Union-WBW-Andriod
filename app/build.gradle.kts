import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets.gradle)
}

/**
 * Where the debug build looks for the backend, as `host:port`.
 *
 * `10.0.2.2:8080` is the emulator's alias for the host machine's loopback, which is right
 * for an emulator and useless on a real phone — a physical device resolving 10.0.2.2 gets
 * nothing. Testing on a handset needs this machine's LAN address instead, and that address
 * changes with the network you are on (home wifi, campus, the event venue), so it is read
 * from `local.properties` rather than committed:
 *
 *     WBW_DEV_HOST=192.168.2.103:8080
 *
 * Absent, it falls back to the emulator alias, so a fresh checkout still behaves as before.
 */
val localProps: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val devApiHost: String =
    localProps.getProperty("WBW_DEV_HOST")?.trim().orEmpty().ifEmpty { "10.0.2.2:8080" }

/**
 * The release keystore, or null on a machine that does not have it.
 *
 * Null is a supported state, not a failure: a fresh checkout — CI, another maintainer,
 * anyone who just wants to run the debug build — has no keystore and no business having
 * one, and must still be able to `assembleDebug` without editing this file. Only
 * `assembleRelease` needs it, and that fails loudly below if it is missing rather than
 * quietly producing an unsigned APK that no phone will install.
 *
 * The keystore path points outside the repository. Every other secret here is a value in a
 * gitignored file; this one is a *file*, and a file that lives outside the working tree
 * cannot be added by a careless `git add -A` no matter what .gitignore says.
 */
val releaseKeystore: File? = localProps.getProperty("WBW_STORE_FILE")
    ?.let { file(it) }
    ?.takeIf { it.exists() }

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

    signingConfigs {
        // Only registered when the keystore is actually present — see `releaseKeystore`.
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = localProps.getProperty("WBW_STORE_PASSWORD")
                keyAlias = localProps.getProperty("WBW_KEY_ALIAS")
                keyPassword = localProps.getProperty("WBW_KEY_PASSWORD")
                // v2 + v3. Not v1: AGP drops JAR signing on its own once minSdk is 24 or
                // above, and minSdk here is 26 — every phone that can install this app
                // verifies v2, so v1 would only add a second signature nobody reads and a
                // slower install. `apksigner verify` on the output reports v1 false, v2 and
                // v3 true, which is the correct shape for this minSdk, not a problem.
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            // Host comes from WBW_DEV_HOST in local.properties — see `devApiHost` above.
            // Defaults to the emulator's host-loopback alias; set it to this machine's LAN
            // address to run the debug build on a real phone.
            buildConfigField("String", "API_BASE_URL", "\"http://$devApiHost/wbw/\"")
        }
        release {
            // Absent on a machine without the keystore, which turns `assembleRelease` into
            // an unsigned APK. That is worse than an error, because it looks like a
            // successful build right up until a phone refuses to install the file, so the
            // check below turns it into one.
            signingConfig = signingConfigs.findByName("release")

            // Left off for now, deliberately. R8 would take a real bite out of the 22MB —
            // most of the APK is a 41MB classes.dex of Compose — but it also rewrites
            // reflective call sites, and this app has two: Retrofit's interface proxies and
            // kotlinx.serialization's generated serializers. Turning it on is a change that
            // needs the whole app re-tested against a real backend, not something to
            // discover from a crash report after a release. Do it as its own piece of work.
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
    implementation(libs.backdrop)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.zxing.core)
}

/**
 * The Maps key lives in local.properties (gitignored) as MAPS_API_KEY, and the secrets
 * plugin injects it as the `MAPS_API_KEY` manifest placeholder the map meta-data reads.
 *
 * `defaultPropertiesFileName` points at a committed file carrying an empty placeholder, so
 * a fresh checkout with no key still builds — the map renders blank tiles with a log
 * warning rather than failing the build. `ignoreList` keeps the plugin from trying to turn
 * `sdk.dir` and the Kotlin daemon settings in local.properties into placeholders, which it
 * cannot, because a dot is illegal in a placeholder name.
 */
/**
 * Refuse to build an unsigned release rather than producing one.
 *
 * Without this the build succeeds, writes `app-release-unsigned.apk`, and the problem only
 * shows up as "App not installed" on somebody's phone — by which point the file may already
 * be attached to a GitHub release and scanned off a poster.
 */
gradle.taskGraph.whenReady {
    val buildingRelease = allTasks.any { it.name == ":app:assembleRelease" || it.name == "assembleRelease" }
    if (buildingRelease && releaseKeystore == null) {
        throw GradleException(
            "Release signing is not configured. Set WBW_STORE_FILE / WBW_STORE_PASSWORD / " +
                "WBW_KEY_ALIAS / WBW_KEY_PASSWORD in local.properties, pointing at the release " +
                "keystore. Without them this would emit an unsigned APK that cannot be installed.",
        )
    }
}

secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
    ignoreList.add("sdk.*")
    ignoreList.add("kotlin.*")
    ignoreList.add("org.gradle.*")
    // Read directly by the build script above, not a manifest placeholder — and the value
    // is a host:port, which is not a legal placeholder name anyway.
    ignoreList.add("WBW_.*")
}
