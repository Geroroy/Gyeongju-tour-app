// Wear OS 앱 모듈. AGP 9 built-in Kotlin 사용 — org.jetbrains.kotlin.android 을 적용하지 않는다.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.geroroy.gyeongjubadge"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.geroroy.gyeongjubadge"
        minSdk = 30 // Wear OS 3 (Android 11) 이상
        targetSdk = 37 // Wear OS 7
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // `./gradlew :wear:installDebug -PuseFakeLocation=true` → 첫 장소 중심의 FakeLocationProvider 주입
        val useFakeLocation = providers.gradleProperty("useFakeLocation").orNull == "true"
        buildConfigField("boolean", "USE_FAKE_LOCATION", useFakeLocation.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    // Compose (BOM 으로 버전 관리)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Compose for Wear OS — Material 3 만 사용 (compose-material / androidx.compose.material* 금지)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.ui.tooling)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
