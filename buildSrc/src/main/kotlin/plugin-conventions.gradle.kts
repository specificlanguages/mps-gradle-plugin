plugins {
    java
    id("com.gradle.plugin-publish")
    id("org.gradle.kotlin.kotlin-dsl")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website.set("https://specificlanguages.com")
    vcsUrl.set("https://github.com/specificlanguages/mps-gradle-plugin")
}

group = "com.specificlanguages"
