import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
    kotlin("plugin.compose")
}

android {
    namespace = "com.luminoverse.animevibe"
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.luminoverse.animevibe"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "JIKAN_URL", "\"${localProperties.getProperty("jikan.url") ?: ""}\"")
        buildConfigField("String", "ANIMERUNWAY_URL", "\"${localProperties.getProperty("animerunway.url") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_URL", "\"${localProperties.getProperty("youtube.url") ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("storeFile") ?: System.getenv("STORE_FILE") ?: "app/release.keystore")
            storePassword = localProperties.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD") ?: ""
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions.apply {
        kotlinCompilerExtensionVersion = "2.1.10"
    }
    hilt {
        enableAggregatingTask = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    tasks.withType<Test> {
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.windowsizeclass)
    implementation(libs.compose.adaptive.navigation.suite)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v286)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Worker
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coil
    implementation(libs.coil.compose)

    // PrettyTime
    implementation(libs.prettytime)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Splash screen
    implementation(libs.androidx.core.splashscreen)

    // Commons text
    implementation(libs.commons.text)

    // ExoPlayer
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.media3.datasource)

    // Chucker
    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // Testing Dependencies
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Mocking Libraries
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk)

    // Coroutines Testing
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

kapt {
    correctErrorTypes = true
}