package com.specificlanguages.mps

import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class MpsBuild : Named {
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
     */
    abstract val buildArtifactsDirectory: DirectoryProperty

    /**
     * The task that forks Ant to run the `generate` target of [buildFile].
     */
    abstract val generateTask: Property<RunAnt>

    /**
     * Other builds that this build depends on. This translates to the dependency on
     * the [assemble task][MainBuild.assembleTask] of the corresponding build.
     */
    abstract val dependencies: SetProperty<MainBuild>

    fun dependsOn(vararg builds: MainBuild) {
        dependencies.addAll(*builds)
    }
}

abstract class TestBuild : MpsBuild() {
    /**
     * The task that forks Ant to run the `check` target of [buildFile]. In MPS-generated Ant files `check` depends on
     * `assemble` and it is not possible to run `check` alone.
     */
    abstract val assembleAndCheckTask: Property<RunAnt>
}

abstract class MainBuild : MpsBuild() {
    /**
     * The task that forks Ant to run the `assemble` target of [buildFile].
     */
    abstract val assembleTask: Property<RunAnt>
}
