plugins {
    `plugin-conventions`
}

version = "1.7.0"

repositories {
    mavenCentral()
    maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":artifact-transforms"))
    implementation("de.itemis.mps.build-backends:launcher:2.3.0.91.50c4cb7")
}

kotlin {
    jvmToolchain(8)
}

gradlePlugin {
    plugins {
        register("mps") {
            id = "com.specificlanguages.mps"
            implementationClass = "com.specificlanguages.MpsPlugin"
            displayName = "MPS Build Plugin"
            description = "Builds JetBrains MPS projects using a simple declarative configuration model"
            tags.set(listOf("jetbrainsMps"))
        }
    }
}
