plugins {
    `plugin-conventions`
}

repositories {
    maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":artifact-transforms"))
    implementation("de.itemis.mps.build-backends:launcher:2.5.2.120.c791da5")
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
