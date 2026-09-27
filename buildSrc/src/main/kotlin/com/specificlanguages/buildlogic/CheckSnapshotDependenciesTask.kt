package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/** Checks dependency versions without requiring the module itself to be ready for publication. */
abstract class CheckSnapshotDependenciesTask : DefaultTask() {

    /** Coordinates (`group:name:version`) of the module's project dependencies. */
    @get:Input
    abstract val dependencyCoordinates: ListProperty<String>

    @TaskAction
    fun check() {
        val snapshots = dependencyCoordinates.get().filter { it.substringAfterLast(':').endsWith("-SNAPSHOT") }
        if (snapshots.isNotEmpty()) {
            throw GradleException(
                "Cannot release with snapshot dependencies; prepare their releases first:\n" +
                    snapshots.joinToString("\n") { "  - $it" })
        }
    }
}
