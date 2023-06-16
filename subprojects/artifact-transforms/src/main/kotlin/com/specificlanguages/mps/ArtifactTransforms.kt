package com.specificlanguages.mps

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.registerTransform
import java.io.File

/**
 * Registers [UnzipMps] artifact transform for transforming `artifactType = zip` ([UNZIP_MPS_FROM_ARTIFACT_TYPE]) into
 * `artifactType = unzipped-mps-distribution` ([UNZIP_MPS_TO_ARTIFACT_TYPE]).
 */
class ArtifactTransforms : Plugin<Project> {
    companion object {
        val UNZIP_MPS_FROM_ARTIFACT_TYPE = "zip"
        val UNZIP_MPS_TO_ARTIFACT_TYPE = "unzipped-mps-distribution"
        val ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String::class.java)

        /**
         * Uses Gradle artifact transform mechanism to extract MPS from [config] and returns the root directory
         * of the extracted archive.
         */
        fun getMpsRoot(config: Configuration): Provider<File> = config.incoming
            .artifactView { attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, UNZIP_MPS_TO_ARTIFACT_TYPE) }
            .files.elements.map { it.single().asFile }
    }

    override fun apply(project: Project) {
        project.dependencies.registerTransform(UnzipMps::class) {
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, UNZIP_MPS_FROM_ARTIFACT_TYPE)
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, UNZIP_MPS_TO_ARTIFACT_TYPE)
        }
    }
}
