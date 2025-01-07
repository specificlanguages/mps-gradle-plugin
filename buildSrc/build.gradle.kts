plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    fun plugin(id: String, version: String) = "$id:$id.gradle.plugin:$version"

    implementation(plugin("com.gradle.plugin-publish", "1.2.1"))
    implementation(plugin("org.jetbrains.kotlin.jvm", embeddedKotlinVersion))
    implementation(plugin("org.gradle.kotlin.kotlin-dsl", "5.1.2"))
    implementation(plugin("org.jetbrains.kotlinx.binary-compatibility-validator", "0.13.2"))
}
