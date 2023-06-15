plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "com.specificlanguages"
version = "1.6.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

kotlin {
    jvmToolchain(8)
}

gradlePlugin {
    website.set("https://specificlanguages.com")
    vcsUrl.set("https://github.com/specificlanguages/mps-gradle-plugin")
    plugins {
        register("mpsPlugin") {
            id = "com.specificlanguages.mps"
            implementationClass = "com.specificlanguages.MpsPlugin"
            displayName = "MPS Build Plugin"
            description = "Builds JetBrains MPS projects using a simple declarative configuration model"
            tags.set(listOf("jetbrainsMps"))
        }
    }
}
