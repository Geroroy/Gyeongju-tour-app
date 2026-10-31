import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 순수 Kotlin JVM 모듈: 도메인 모델 + 인증 규칙. Android 의존성 금지 (CLAUDE.md).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        // :wear 의 compileOptions(17)와 맞춰 Gradle 변형 속성(org.gradle.jvm.version)이 일치하도록 한다.
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
