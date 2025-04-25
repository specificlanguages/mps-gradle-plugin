package com.specificlanguages.mps

import com.specificlanguages.mps.internal.ParsingResult
import com.specificlanguages.mps.internal.readSolutionName
import de.itemis.mps.gradle.launcher.MpsBackendLauncher
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "needs more information")
abstract class GenerateBuildScripts : DefaultTask() {

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Internal("TODO")
    abstract val mpsHome: DirectoryProperty

    /**
     * Maps a project directory to a collection of build solution descriptors.
     */
    @get:Internal
    abstract val buildSolutionDescriptorsByProject: MapProperty<Directory, List<RegularFile>>

    @get:Classpath
    abstract val generateBackendClasspath: ConfigurableFileCollection

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Internal("not considered inputs")
    abstract val pathVariables: MapProperty<String, File>

    /**
     * Environment variables for the forked JVM. Defaults to the environment of the current process.
     */
    @get:Internal
    abstract val environment: MapProperty<String, String>

    init {
        environment.putAll(project.providers.environmentVariablesPrefixedBy(""))
    }

    @TaskAction
    fun make() {
        temporaryDir.deleteRecursively()

        for ((projectDirectory, buildSolutionDescriptors) in buildSolutionDescriptorsByProject.get()) {
            val moduleNames = buildSolutionDescriptors.map { file ->
                readSolutionName(file.asFile).let {
                    when (it) {
                        is ParsingResult.Error -> throw GradleException("Could not read build solution name from $file: ${it.message}")
                        is ParsingResult.Value -> it.value
                    }
                }
            }

            execOperations.javaexec {
                args("--project=${projectDirectory}", "--environment=MPS")

                for (moduleName in moduleNames) {
                    args("--module=${Regex.escape(moduleName)}")
                }

                for (entry in pathVariables.get()) {
                    args("--macro=${entry.key}::${entry.value}")
                }

                group = "build"
                description = "Generate Ant build script from a solution"

                classpath(objects.fileTree().from(mpsHome).include("lib/**/*.jar"))
                classpath(generateBackendClasspath)

                @Suppress("USELESS_CAST")
                environment = this@GenerateBuildScripts.environment.get() as Map<String, *>

                mainClass.set("de.itemis.mps.gradle.generate.MainKt")

                MpsBackendLauncher(project.objects).builder()
                    .withJavaLauncher(javaLauncher)
                    .withMpsHome(mpsHome.asFile)
                    .withTemporaryDirectory(temporaryDir)
                    .configure(this)
            }
        }
    }
}
