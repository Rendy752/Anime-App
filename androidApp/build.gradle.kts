import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Properties

plugins {
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.luminoverse.animevibe.android"
    compileSdk = 36

    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.luminoverse.animevibe"
        minSdk = 27
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "JIKAN_URL",
            "\"${localProperties.getProperty("jikan.url") ?: ""}\""
        )
        buildConfigField(
            "String",
            "ANIMERUNWAY_URL",
            "\"${localProperties.getProperty("animerunway.url") ?: ""}\""
        )
        buildConfigField(
            "String",
            "YOUTUBE_URL",
            "\"${localProperties.getProperty("youtube.url") ?: ""}\""
        )

        ndk {
            debugSymbolLevel = "FULL"
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword =
                localProperties.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD")
                        ?: ""
            keyAlias = localProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS") ?: ""
            keyPassword =
                localProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD") ?: ""
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
            ndk.debugSymbolLevel = "FULL"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
        }
        jniLibs {
            keepDebugSymbols += "**/lib*.so"
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
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.windowsizeclass)
    implementation(libs.androidx.compose.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ViewModel (Android-specific lifecycle)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room (Android-specific persistence)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Worker (Android-specific background tasks)
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines (Android-specific dispatchers if needed, core is in shared)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit (if you keep it Android-specific for some reason, otherwise move to shared with Ktor)
    // If you plan to use Retrofit in shared, you need a multiplatform Http client like Ktor.
    // If Retrofit remains only for Android, keep it here.
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Coil (Android-specific image loading)
    implementation(libs.coil.compose)

    // PrettyTime (if not moved to shared with a multiplatform alternative)
    implementation(libs.prettytime)

    // Hilt (Android-specific DI)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)

    // Kotlinx Serialization (core is in shared, Android-specific if needed)
    // `kotlinx.serialization.json` should be in shared

    // Splash screen (Android-specific)
    implementation(libs.androidx.core.splashscreen)

    // Commons text (if not moved to shared)
    implementation(libs.commons.text)

    // ExoPlayer (Android-specific media playback)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)

    // Chucker (Android-specific debugging tool)
    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // Testing Dependencies (Android-specific)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.slf4j.simple)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Mocking Libraries
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk)

    // Coroutines Testing
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Play store
    implementation(libs.app.update.ktx)
}

kapt {
    correctErrorTypes = true
}