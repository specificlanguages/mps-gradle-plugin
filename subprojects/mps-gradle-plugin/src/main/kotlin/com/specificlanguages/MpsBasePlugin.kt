package com.specificlanguages

import com.specificlanguages.jbrtoolchain.JbrToolchainPlugin
import com.specificlanguages.mps.ArtifactTransforms
import org.gradle.api.Plugin
import org.gradle.api.Project

class MpsBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(ArtifactTransforms::class.java)
            pluginManager.apply(JbrToolchainPlugin::class.java)

            val mpsConfiguration = configurations.create("mps") {
                isCanBeResolved = true
                isCanBeConsumed = false
            }
        }
    }
}
