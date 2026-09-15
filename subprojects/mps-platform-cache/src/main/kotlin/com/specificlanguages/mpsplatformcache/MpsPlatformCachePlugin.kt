package com.specificlanguages.mpsplatformcache

import org.gradle.api.Plugin
import org.gradle.api.Project

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
