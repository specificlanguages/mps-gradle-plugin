import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"

    implementation(plugin("com.gradle.plugin-publish", "2.0.0"))
    implementation(plugin("org.jetbrains.kotlin.jvm", embeddedKotlinVersion))
    implementation(plugin("org.gradle.kotlin.kotlin-dsl", expectedKotlinDslPluginsVersion))
    implementation(plugin("org.jetbrains.kotlinx.binary-compatibility-validator", "0.17.0"))
}
