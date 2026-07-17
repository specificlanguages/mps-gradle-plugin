plugins {
    `plugin-conventions`
}

gradlePlugin {
    plugins {
        register("artifactTransforms") {
            id = "com.specificlanguages.mps.artifact-transforms"
            implementationClass = "com.specificlanguages.mps.ArtifactTransforms"
            displayName = "MPS Artifact Transforms"
            description = "Artifact transforms that help share MPS distribution among multiple projects"
            tags = listOf("jetbrainsMps", "artifactTransform")
        }
    }
}
