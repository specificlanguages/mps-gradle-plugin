package com.specificlanguages.buildlogic

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CheckSnapshotDependenciesTaskTest {
    @TempDir
    lateinit var temporaryFolder: File

    private fun checkDependencies(vararg coordinates: String) {
        val project = ProjectBuilder.builder().withProjectDir(temporaryFolder).build()
        project.version = "2.0.0-SNAPSHOT"
        project.tasks.register("checkSnapshotDependencies", CheckSnapshotDependenciesTask::class.java).get().apply {
            dependencyCoordinates.set(coordinates.toList())
        }.check()
    }

    @Test
    fun `snapshot module with no dependencies can prepare a release`() {
        checkDependencies()
    }

    @Test
    fun `release dependencies need not be published to prepare a release`() {
        checkDependencies("example.invalid:unpublished:1.0.0")
    }

    @Test
    fun `reports every snapshot dependency and excludes release dependencies`() {
        val message = assertThrows<GradleException> {
            checkDependencies("example:first:1.0.0-SNAPSHOT", "example:released:1.0.0", "example:second:2.0.0-SNAPSHOT")
        }.message!!
        assertTrue(message.contains("example:first:1.0.0-SNAPSHOT"))
        assertTrue(message.contains("example:second:2.0.0-SNAPSHOT"))
        assertFalse(message.contains("example:released:1.0.0"))
    }
}
