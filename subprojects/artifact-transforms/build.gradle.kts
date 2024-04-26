plugins {
    `plugin-conventions`
}

group = "com.specificlanguages"
version = "1.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("artifactTransforms") {
            id = "com.specificlanguages.mps.artifact-transforms"
            implementationClass = "com.specificlanguages.mps.ArtifactTransforms"
            displayName = "MPS Artifact Transforms"
            description = "Artifact transforms that help share MPS distribution among multiple projects"
            tags.set(listOf("jetbrainsMps", "artifactTransform"))
        }
    }
}
