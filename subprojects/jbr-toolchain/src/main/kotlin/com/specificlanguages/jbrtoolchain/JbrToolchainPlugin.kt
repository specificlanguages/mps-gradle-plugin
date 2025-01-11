package com.specificlanguages.jbrtoolchain

import com.specificlanguages.jbrtoolchain.internal.ExtractJbrTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec
import java.io.File

abstract class JbrToolchainPlugin : Plugin<Project> {

    companion object {
        private const val EXTRACT_JBR_FROM_ARTIFACT_TYPE = "tgz"
        private const val EXTRACT_JBR_TO_ARTIFACT_TYPE = "extracted-jbr-directory"

        private const val OSX = "osx"
    }

    private fun getExtractedDirectory(config: Configuration): File = config.incoming
        .artifactView {
            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                EXTRACT_JBR_TO_ARTIFACT_TYPE
            )
        }
        .files.singleFile

    private fun getJavaHomeFromExtractedDirectory(os: String, root: File) =
        when (os) {
            OSX -> root.resolve("Contents/Home")
            else -> root
        }

    override fun apply(project: Project) {
        project.run {
            val os = currentOs()
            val arch = currentArch()

            val jbrConfig = configurations.register("jbr") {
                isCanBeConsumed = false

                resolutionStrategy.dependencySubstitution {
                    all {
                        artifactSelection {
                            selectArtifact(EXTRACT_JBR_FROM_ARTIFACT_TYPE, "tgz", "$os-$arch")
                        }
                    }
                }
            }

            dependencies.registerTransform(ExtractJbrTransform::class.java) {
                from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, EXTRACT_JBR_FROM_ARTIFACT_TYPE)
                to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, EXTRACT_JBR_TO_ARTIFACT_TYPE)
            }

            val jbrSpec = jbrConfig.map {
                val javaHome = getJavaHomeFromExtractedDirectory(os, getExtractedDirectory(it))
                SpecificInstallationToolchainSpec.fromJavaHome(objects, javaHome)
            }

            extensions.create("jbrToolchain", JbrToolchainExtension::class.java, jbrSpec)
        }
    }

    private fun currentOs(): String = System.getProperty("os.name").let {
        val osName = it.lowercase()
        when {
            osName.contains("windows") -> "windows"
            osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx") -> OSX
            osName.contains("linux") -> "linux"
            else -> throw IllegalStateException("Unsupported value of os.name system property: $it")
        }
    }

    private fun currentArch(): String = System.getProperty("os.arch").let {
        when (it) {
            "x86_64", "amd64" -> "x64"
            "aarch64", "arm64" -> "aarch64"
            else -> throw IllegalStateException("Unsupported value of os.arch system property: $it")
        }
    }
}
