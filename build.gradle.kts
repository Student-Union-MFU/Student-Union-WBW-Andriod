// Top-level build file — plugins are declared here (apply false) and applied per-module.
// AGP 9 ships built-in Kotlin support, so there is no separate kotlin.android
// plugin — only the compiler plugins (Compose, kotlinx.serialization) are added.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.secrets.gradle) apply false
}
