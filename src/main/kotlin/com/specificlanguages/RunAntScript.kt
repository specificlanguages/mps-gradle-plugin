package com.specificlanguages

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

/**
 * Runs an Ant script via [org.gradle.api.Project.javaexec]. The plugin configures some conventions for this task type
 * based on [MpsDefaultsExtension].
 */
internal abstract class RunAntScript : DefaultTask() {
    /**
     * The Ant build script. Defaults to [MpsDefaultsExtension.buildScript]
     */
    @get:InputFile
    abstract val buildScript: RegularFileProperty

    /**
     * The targets to execute.
     */
    @get:Input
    abstract val targets: ListProperty<String>

    /**
     * The classpath. Would typically be a configuration including Ant and any extra libraries with tasks, such as
     * ant-junit or ant-contrib. Defaults to `ant` configuration.
     */
    @get:InputFiles
    abstract val classpath: Property<FileCollection>

    /**
     * Ant properties to pass to the script (`-Dkey=value`). Defaults to the following properties:
     * * `mps_home`: [MpsDefaultsExtension.mpsHome]
     * * `version`: project version
     */
    @get:Input
    abstract val antProperties: MapProperty<String, String>

    /**
     * Any additional arguments to the script. Defaults to empty.
     */
    @get:Input
    abstract val scriptArgs: ListProperty<String>

    /**
     * Actions that will be called after the internally used [JavaExecSpec] is configured according to other properties.
     * Defaults to empty.
     */
    @get:Input
    abstract val additionalConfigurationActions: ListProperty<Action<in JavaExecSpec>>

    /**
     * Configure the Java execution of Ant. The action will be called after the internally used [JavaExecSpec] is
     * configured according to other properties. Actions are called in the order they are registered.
     */
    fun configureJavaExec(action: Action<in JavaExecSpec>) {
        additionalConfigurationActions.add(action)
    }

    @TaskAction
    open fun build() {
        project.javaexec {
            mainClass.set("org.apache.tools.ant.launch.Launcher")
            workingDir = project.projectDir
            classpath = this@RunAntScript.classpath.get()

            argumentProviders.add {
                val args = mutableListOf<String>()
                args.run {
                    addAll(antProperties.get().map { "-D${it.key}=${it.value}" })
                    addAll(scriptArgs.get())
                    add("-buildfile")
                    add(buildScript.get().asFile.path)
                    addAll(targets.get())
                }
                args
            }

            additionalConfigurationActions.get().forEach { it.execute(this) }
        }
    }
}
