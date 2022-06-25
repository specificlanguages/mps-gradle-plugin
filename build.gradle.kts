plugins {
    java
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.16.0"
}

group = "com.specificlanguages"
version = "1.4.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

gradlePlugin {
    plugins {
        register("mpsPlugin") {
            id = "com.specificlanguages.mps"
            implementationClass = "com.specificlanguages.MpsPlugin"
            displayName = "MPS Build Plugin"
            description = "Builds JetBrains MPS projects using a simple declarative configuration model"
        }
    }
}

pluginBundle {
    website = "https://specificlanguages.com"
    vcsUrl = "https://github.com/specificlanguages/mps-gradle-plugin"
    tags = listOf("jetbrainsMps")
}
