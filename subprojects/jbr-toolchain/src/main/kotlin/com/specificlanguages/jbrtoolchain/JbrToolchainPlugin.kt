package com.specificlanguages.jbrtoolchain

import com.specificlanguages.jbrtoolchain.internal.JbrOsArch
import com.specificlanguages.mpsplatformcache.MpsPlatformCachePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector

abstract class JbrToolchainPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(MpsPlatformCachePlugin::class.java)

            val jbrOsArch = JbrOsArch.current()

            val jbrConfig = configurations.register("jbr") {
                isCanBeConsumed = false

                resolutionStrategy.dependencySubstitution {
                    all {
                        // Add the platform classifier to the JBR distribution artifact only. A marker such as
                        // com.jetbrains.mps:mps-jbr carries no platform-specific archive, it only declares a
                        // dependency on the matching JBR version, so classifier selection must skip it and apply
                        // to the JBR it depends on instead.
                        val requested = requested
                        if (requested is ModuleComponentSelector && isJbrDistribution(requested)) {
                            artifactSelection {
                                selectArtifact("tgz", null, jbrOsArch.classifier)
                            }
                        }
                    }
                }
            }

            val toolchainSpecFactory = objects.newInstance(ToolchainSpecFactory::class.java)
            val jbrSpec = MpsPlatformCachePlugin.getMpsPlatformCache(project).getJbrRoot(jbrConfig).map {
                val javaHome = jbrOsArch.getJavaHomeFromExtractedDirectory(it)
                toolchainSpecFactory.fromJavaHome(javaHome)
            }

            extensions.create("jbrToolchain", JbrToolchainExtension::class.java, jbrSpec)
        }
    }

    private fun isJbrDistribution(selector: ModuleComponentSelector): Boolean =
        selector.group == "com.jetbrains.jdk"

}
