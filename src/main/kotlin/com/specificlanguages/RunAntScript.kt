package com.specificlanguages

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

open class RunAntScript : DefaultTask() {
    @InputFile
    lateinit var script: Any

    @Input
    var targets: List<String> = emptyList()

    @InputFiles
    var scriptClasspath: FileCollection? = null

    @Input
    var scriptArgs: List<String> = emptyList()

    fun targets(vararg targets: String) {
        this.targets = targets.toList()
    }

    @TaskAction
    fun build() {
        val allArgs = scriptArgs

        project.javaexec {
            mainClass.set("org.apache.tools.ant.launch.Launcher")
            workingDir = project.projectDir

            if (scriptClasspath != null) {
                classpath(scriptClasspath)
            }

            args(allArgs)
            args("-buildfile", project.file(script))
            args(targets)
        }
    }
}