package com.specificlanguages.mpsplatformcache

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry

class MpsPlatformCachePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("mpsPlatformCache", MpsPlatformCache::class.java)
    }

    companion object {
        @JvmStatic
        fun getMpsPlatformCache(project: Project): MpsPlatformCache =
            project.extensions.findByType(MpsPlatformCache::class.java)!!
    }
}
