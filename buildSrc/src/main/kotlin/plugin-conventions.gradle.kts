plugins {
    java
    id("com.gradle.plugin-publish")
    id("org.gradle.kotlin.kotlin-dsl")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

gradlePlugin {
    website.set("https://specificlanguages.com")
    vcsUrl.set("https://github.com/specificlanguages/mps-gradle-plugin")
}

group = "com.specificlanguages"

repositories {
    mavenCentral()
}
