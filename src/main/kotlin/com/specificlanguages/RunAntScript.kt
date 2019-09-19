package com.specificlanguages

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

open class RunAntScript : DefaultTask() {
    lateinit var script: Any
    var targets: List<String> = emptyList()
    var scriptClasspath: FileCollection? = null
    var scriptArgs: List<String> = emptyList()

    fun targets(vararg targets: String) {
        this.targets = targets.toList()
    }

    @TaskAction
    fun build() {
        val allArgs = scriptArgs

        project.javaexec {
            main = "org.apache.tools.ant.launch.Launcher"
            workingDir = project.rootDir

            if (scriptClasspath != null) {
                classpath(scriptClasspath)
            }

            args(allArgs)
            args("-buildfile", project.file(script))
            args(targets)
        }
    }
}