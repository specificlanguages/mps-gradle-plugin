package com.specificlanguages.mps

import com.specificlanguages.mps.RunAnt
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

abstract class MpsBuild @Inject constructor(private val name: String) : Named {
    override fun getName(): String = name

    /**
     * The .msd file of the build solution.
     */
    abstract val buildSolutionDescriptor: RegularFileProperty

    /**
     * The name of the build project (the root node) in MPS.
     */
    abstract val buildProjectName: Property<String>

    /**
     * The generated Ant script.
     */
    abstract val buildFile: RegularFileProperty

    /**
     * The task that forks Ant to run the `generate` target of [buildFile].
     */
    abstract val generateTask: Property<RunAnt>

    /**
     * Other builds that this build depends on. This translates to the dependency on
     * the [assemble task][MainBuild.assembleTask] of the corresponding build.
     */
    abstract val dependencies: SetProperty<MainBuild>

    /**
     * The directory where the build places its artifacts. Defaults to `$buildDir/artifacts/`[buildProjectName].
     */
    abstract val artifactsDirectory: DirectoryProperty

    fun dependsOn(vararg builds: MainBuild) {
        dependencies.addAll(*builds)
    }
}

abstract class TestBuild(name: String) : MpsBuild(name) {
    /**
     * The task that forks Ant to run the `check` target of [buildFile]. In MPS-generated Ant files `check` depends on
     * `assemble` and it is not possible to run `check` alone.
     */
    abstract val assembleAndCheckTask: Property<RunAnt>
}

abstract class MainBuild(name: String) : MpsBuild(name) {
    /**
     * The task that forks Ant to run the `assemble` target of [buildFile].
     */
    abstract val assembleTask: Property<RunAnt>

    /**
     * The task to package the generated artifacts into a zip file.
     */
    abstract val packageTask: Property<Zip>
}
