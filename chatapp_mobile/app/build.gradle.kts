plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // ĐÃ GỠ BỎ GOOGLE SERVICES PLUGIN
}

android {
    namespace = "com.example.chatapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.chatapp"
        minSdk = 26
        targetSdk = 36
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
            val mobileUrl = System.getenv("MOBILE_BASE_URL") ?: "http://localhost:8081/"
            buildConfigField("String", "BASE_URL", "\"$mobileUrl\"")
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core Android & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.runtime:runtime:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // ĐÃ GỠ BỎ TOÀN BỘ FIREBASE DEPENDENCIES

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Networking: Retrofit & Gson (Dùng cho JWT Auth và API History)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Networking: STOMP & RxJava (Dùng cho Chat Realtime qua WebSocket)
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
