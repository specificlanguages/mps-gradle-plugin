import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version "1.3.41"
    id("com.gradle.plugin-publish") version "0.16.0"
}

group = "com.specificlanguages"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    wrapper {
        gradleVersion = "5.6.2"
        distributionType = Wrapper.DistributionType.ALL
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        register("mpsPlugin") {
            id = "com.specificlanguages.mps"
            implementationClass = "com.specificlanguages.MpsPlugin"
            displayName = "MPS Build Plugin"
            description = "A simple plugin for building JetBrains MPS projects"
        }
    }
}

pluginBundle {
    website = "https://specificlanguages.com"
    vcsUrl = "https://github.com/specificlanguages/mps-gradle-plugin"
    tags = listOf("jetbrainsMps")
}
