package com.specificlanguages.mps

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

abstract class MpsBuild(
    /**
     * The task that forks Ant to run the `generate` target of [buildFile].
     */
    val generateTask: TaskProvider<out RunAnt>
) : Named {
    @get:Inject
    protected abstract val layout: ProjectLayout

    /**
     * The MPS project that contains the build solution
     */
    abstract val mpsProjectDirectory: DirectoryProperty

    init {
        mpsProjectDirectory.convention(layout.projectDirectory)
    }

    /**
     * The .msd file of the build solution.
     */
    abstract val buildSolutionDescriptor: RegularFileProperty

    /**
     * The generated Ant script.
     */
    abstract val buildFile: RegularFileProperty

    /**
     * The directory where the build places its artifacts. Typically `$buildDir/artifacts/[build-project-name]`.
     * `build.layout` Ant property will be set to this directory.
     */
    abstract val buildArtifactsDirectory: DirectoryProperty

    /**
     * Other builds that this build depends on. This translates to the dependency on
     * the [assemble task][MainBuild.assembleTask] of the corresponding build.
     */
    abstract val dependencies: SetProperty<MainBuild>

    fun dependsOn(vararg builds: MainBuild) {
        dependencies.addAll(*builds)
    }
}

abstract class MainBuild @Inject constructor(
    generateTask: TaskProvider<out RunAnt>,
    /**
     * The task that forks Ant to run the `assemble` target of [buildFile].
     */
    val assembleTask: TaskProvider<out RunAnt>
) : MpsBuild(generateTask) {

    /**
     * Indicates whether the build is internal or published (packaged). Published builds have their artifacts included
     * in the zip produced by the `zip` task and published by `components["mps"]`. By default, all main builds
     * are published.
     */
    abstract val published: Property<Boolean>

    init {
        published.convention(true)
    }
}

abstract class TestBuild @Inject constructor(
    generateTask: TaskProvider<out RunAnt>,
    /**
     * The task that forks Ant to run the `check` target of [buildFile]. In MPS-generated Ant files `check` depends on
     * `assemble` and it is not possible to run `check` alone.
     */
    val assembleAndCheckTask: TaskProvider<out RunAnt>
) : MpsBuild(generateTask)
