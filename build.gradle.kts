// Top-level build file where you can add configuration options common to all sub-projects/modules.
// se dejan en false, pero disponibles para usar cuando se active
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}