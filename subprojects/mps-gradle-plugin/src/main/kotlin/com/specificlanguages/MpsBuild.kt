package com.specificlanguages

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

abstract class MpsBuild @Inject constructor(private val name: String) : Named {
    override fun getName(): String = name

    abstract val buildSolutionDescriptor: RegularFileProperty
    abstract val buildProjectName: Property<String>
    abstract val generatedBuildFile: RegularFileProperty

    abstract val generateTask: Property<RunAnt>

    abstract val dependencies: SetProperty<MainBuild>

    abstract val artifactsDirectory: DirectoryProperty

    fun dependsOn(vararg builds: MainBuild) {
        dependencies.addAll(*builds)
    }
}

abstract class TestBuild(name: String) : MpsBuild(name) {
    abstract val assembleAndCheckTask: Property<RunAnt>
}

abstract class MainBuild(name: String) : MpsBuild(name) {
    abstract val assembleTask: Property<RunAnt>
    abstract val packageTask: Property<Zip>
}
