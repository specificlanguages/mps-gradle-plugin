package com.specificlanguages.jbrtoolchain

import com.specificlanguages.jbrtoolchain.internal.JbrOsArch
import com.specificlanguages.mpsplatformcache.MpsPlatformCachePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class JbrToolchainPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(MpsPlatformCachePlugin::class.java)

            val jbrOsArch = JbrOsArch.current()

            val jbrConfig = configurations.register("jbr") {
                isCanBeConsumed = false

                resolutionStrategy.dependencySubstitution {
                    all {
                        artifactSelection {
                            selectArtifact("tgz", null, jbrOsArch.classifier)
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

}
