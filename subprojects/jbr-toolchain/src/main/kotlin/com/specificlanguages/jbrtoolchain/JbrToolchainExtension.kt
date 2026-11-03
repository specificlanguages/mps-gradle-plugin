package com.specificlanguages.jbrtoolchain

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import javax.inject.Inject

@Suppress("Unused", "CanBeParameter")
abstract class JbrToolchainExtension(val spec: Provider<JavaToolchainSpec>) {
    @get:Inject
    abstract val javaToolchainService: JavaToolchainService

    val javaLauncher = spec.flatMap(javaToolchainService::launcherFor)

    /**
     * Makes [configuration] resolve JBR distributions (`com.jetbrains.jdk` modules) to the archive for [platform].
     * This applies both to JBRs declared directly and to JBRs reached through a marker artifact such as
     * `com.jetbrains.mps:mps-jbr`.
     */
    fun selectPlatform(configuration: Configuration, platform: JbrPlatform) {
        configuration.resolutionStrategy.dependencySubstitution {
            all {
                // Add the platform classifier to the JBR distribution artifact only. A marker such as
                // com.jetbrains.mps:mps-jbr carries no platform-specific archive, it only declares a
                // dependency on the matching JBR version, so classifier selection must skip it and apply
                // to the JBR it depends on instead.
                val requested = requested
                if (requested is ModuleComponentSelector && isJbrDistribution(requested)) {
                    artifactSelection {
                        selectArtifact("tgz", null, platform.classifier)
                    }
                }
            }
        }
    }

    private fun isJbrDistribution(selector: ModuleComponentSelector): Boolean =
        selector.group == "com.jetbrains.jdk"
}
