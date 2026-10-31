// Kotlin JVM / Compose compiler / KSP 플러그인을 루트에서 한 번 선언해 KGP 2.4.21이
// 루트 클래스패스 한 곳에 놓이게 한다(AGP 9 built-in Kotlin은 상위 클래스로더의 KGP를 사용).
// AGP(com.android.application)와 Room Gradle Plugin은 :wear 에서만 적용한다.
// 덕분에 Android SDK가 없는 환경에서도 `./gradlew :core:test --configure-on-demand` 가 동작한다.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
