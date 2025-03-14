package com.specificlanguages.mps

import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.ExecOperations
import org.gradle.process.JavaExecSpec
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

/**
 * Forks Ant to run [buildFile] with [targets].
 */
@DisableCachingByDefault(because = "Needs additional information about inputs and outputs")
@Incubating
abstract class RunAnt : DefaultTask() {

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val toolchains: JavaToolchainService

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:InputFile
    abstract val buildFile: RegularFileProperty

    @get:Input
    abstract val targets: ListProperty<String>

    /**
     * Path-valued properties (e.g. MPS home, dependencies location, etc.). Not considered task inputs, i.e. the task
     * will not be re-run if a property is added, changed, or removed.
     */
    @get:Internal("not considered inputs")
    abstract val pathProperties: MapProperty<String, File>

    /**
     * Value properties (e.g. build numbers, version numbers, etc.). Considered task inputs.
     */
    @get:Input
    abstract val valueProperties: MapProperty<String, String>

    /**
     * Extra Ant options, e.g. `-silent`, `-verbose` and the like.
     */
    @get:Input
    abstract val options: ListProperty<String>

    /**
     * Arguments for the forked JVM.
     */
    @get:Internal
    abstract val jvmArguments: ListProperty<String>

    /**
     * Environment variables for the forked JVM. Defaults to the environment of the current process.
     */
    @get:Internal
    abstract val environment: MapProperty<String, String>

    /**
     * Working directory for the forked JVM. Default is the Gradle-provided temporary directory of the task.
     */
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    init {
        javaLauncher.convention(toolchains.launcherFor({
            // "A specification without a language version, in most cases, would be treated as a one that selects
            // the toolchain of the current build."
            //
            // https://docs.gradle.org/current/userguide/toolchains.html#sec:configuring_toolchain_specifications
        }))

        environment.putAll(project.providers.environmentVariablesPrefixedBy(""))

        workingDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun build() {
        execOperations.javaexec {
            workingDir = this@RunAnt.workingDirectory.get().asFile
            executable = this@RunAnt.javaLauncher.get().executablePath.toString()

            @Suppress("USELESS_CAST")
            environment = this@RunAnt.environment.get() as Map<String, Any>

            jvmArgs = this@RunAnt.jvmArguments.get()
            classpath = this@RunAnt.classpath

            mainClass.set("org.apache.tools.ant.launch.Launcher")
            args = this@RunAnt.buildAntArguments()
        }
    }

    private fun buildAntArguments(): List<String?> = buildList {
        addAll(pathProperties.get().map { "-D${it.key}=${it.value}" })
        addAll(valueProperties.get().map { "-D${it.key}=${it.value}" })

        addAll(options.get())

        add("-buildfile")
        add(buildFile.get().toString())
        addAll(targets.get())
    }
}