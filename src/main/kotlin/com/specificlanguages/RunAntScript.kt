package com.specificlanguages

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

internal open class RunAntScript @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @InputFile
    lateinit var script: Any

    @Input
    val targets: ListProperty<String> = objects.listProperty()

    @InputFiles
    var scriptClasspath: Any? = null

    @Input
    val scriptArgs: ListProperty<String> = objects.listProperty()

    @TaskAction
    fun build() {

        project.javaexec {
            mainClass.set("org.apache.tools.ant.launch.Launcher")
            workingDir = project.projectDir

            if (scriptClasspath != null) {
                classpath(scriptClasspath)
            }

            argumentProviders.add {
                val args = mutableListOf<String>()
                args.run {
                    addAll(scriptArgs.get())
                    add("-buildfile")
                    add(project.file(script).toString())
                    addAll(targets.get())
                }
                args
            }
        }
    }
}
