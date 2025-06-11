import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.sqldelight)
    id("com.google.devtools.ksp")
}

compose {}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.luminoverse.animevibe.shared.cache")
        }
    }
}

android {
    namespace = "com.luminoverse.animevibe.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 27
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)

                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)

                api(libs.ktor.client.core)
                api(libs.ktor.client.contentNegotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.client.logging)

                api(libs.sqldelight.runtime)
                api(libs.sqldelight.coroutinesExtensions)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.compose.activity)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.driverAndroid)
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.coil.compose)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(compose.html.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.androidx.core.testing)
                implementation(libs.mockk)
            }
        }
        val jsTest by getting {
            dependencies {}
        }
    }
}