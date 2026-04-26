plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

        // ✅ Cấu hình mặc định cho tất cả (sẽ bị ghi đè bởi buildType)
        val defaultUrl = "http://chatapp-nexus-alb-1541337314.ap-southeast-1.elb.amazonaws.com/"
        buildConfigField("String", "BASE_URL", "\"$defaultUrl\"")
        buildConfigField("String", "WS_URL", "\"ws://${defaultUrl.replace("http://", "")}ws/websocket\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // ✅ Đọc từ biến môi trường GitHub Actions
            val mobileUrl = System.getenv("MOBILE_BASE_URL") ?: "http://chatapp-nexus-alb-1541337314.ap-southeast-1.elb.amazonaws.com/"
            buildConfigField("String", "BASE_URL", "\"$mobileUrl\"")
            buildConfigField("String", "WS_URL", "\"ws://${mobileUrl.replace("http://", "").replace("/", "")}/ws/websocket\"")
        }
        debug {
            // Cấu hình khi bạn bấm Run từ Android Studio vào LDPlayer
            val debugUrl = "http://chatapp-nexus-alb-1541337314.ap-southeast-1.elb.amazonaws.com/"
            buildConfigField("String", "BASE_URL", "\"$debugUrl\"")
            buildConfigField("String", "WS_URL", "\"ws://${debugUrl.replace("http://", "").replace("/", "")}/ws/websocket\"")
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
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
