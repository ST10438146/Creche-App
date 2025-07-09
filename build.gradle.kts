// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.2" apply false // Or com.android.library
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    alias(libs.plugins.google.gms.google.services) apply false //
}