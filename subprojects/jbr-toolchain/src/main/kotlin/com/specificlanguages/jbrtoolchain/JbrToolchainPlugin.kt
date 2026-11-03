package com.specificlanguages.jbrtoolchain

import com.specificlanguages.mpsplatformcache.MpsPlatformCachePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class JbrToolchainPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(MpsPlatformCachePlugin::class.java)

            val jbrPlatform = JbrPlatform.current()

            val jbrConfig = configurations.register("jbr")

            val toolchainSpecFactory = objects.newInstance(ToolchainSpecFactory::class.java)
            val jbrSpec = MpsPlatformCachePlugin.getMpsPlatformCache(project).getJbrRoot(jbrConfig).map {
                val javaHome = jbrPlatform.getJavaHomeFromExtractedDirectory(it)
                toolchainSpecFactory.fromJavaHome(javaHome)
            }

            val jbrToolchain = extensions.create("jbrToolchain", JbrToolchainExtension::class.java, jbrSpec)

            jbrConfig.configure {
                isCanBeConsumed = false
                jbrToolchain.selectPlatform(this, jbrPlatform)
            }
        }
    }
}
