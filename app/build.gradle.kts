plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "1.9.0-1.0.11"
    id("kotlinx-serialization")
}

android {
    namespace = "com.example.animeappkotlin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.animeappkotlin"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v286)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx.v282)
    implementation(libs.androidx.navigation.ui.ktx.v282)

    // Glide
    implementation(libs.glide)
    ksp(libs.compiler)

    //PrettyTime
    implementation(libs.prettytime)

    //Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    //ViewModel injection
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.fragment)

    //Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    //Loading Skeleton
    implementation(libs.shimmer)

    //Splash screen
    implementation(libs.androidx.core.splashscreen)

    //Commons text
    implementation(libs.commons.text)

    //Chucker
    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // --- Testing Dependencies ---
    testImplementation("junit:junit:4.13.2") // JUnit for unit testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // AndroidX Test for UI testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Espresso for UI testing

    // Mocking Libraries
    testImplementation("org.mockito:mockito-core:5.3.1") // Mockito for mocking
    implementation("org.mockito:mockito-core:5.3.1") // Mockito for mocking
//    testImplementation("org.mockito:mockito-inline:5.3.1") // Mockito for mocking final classes and methods
//    androidTestImplementation("org.mockito:mockito-inline:5.2.0") // Mockito for mocking final classes and methods
    androidTestImplementation("org.mockito:mockito-android:5.3.1") // Mockito for Android testing

    // --- Architecture Components Testing ---
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0") // For testing LiveData and other Architecture Components

    // --- Coroutines Testing ---
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    testImplementation("net.bytebuddy:byte-buddy:1.14.8")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.14.8")
    implementation("androidx.multidex:multidex:2.0.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
}
