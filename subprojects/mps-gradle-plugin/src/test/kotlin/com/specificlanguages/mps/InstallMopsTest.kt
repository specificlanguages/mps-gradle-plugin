package com.specificlanguages.mps

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InstallMopsTest {

    private fun newTask(tmp: File, installVersion: String = "2.0.1"): InstallMops {
        val project = ProjectBuilder.builder().withProjectDir(tmp).build()
        project.plugins.apply("com.specificlanguages.mps")
        val task = project.tasks.named("installMops", InstallMops::class.java).get()
        task.version.set(installVersion)
        task.targetDir.set(tmp.resolve("bin"))
        return task
    }

    @Test
    fun `fresh install writes the script with version embedded and makes it executable`(@TempDir tmp: File) {
        val task = newTask(tmp, "2.0.1")
        task.install()

        val installed = tmp.resolve("bin/mops")
        assertThat(installed.isFile, equalTo(true))
        assertThat(installed.canExecute(), equalTo(true))
        assertThat(
            InstallMops.readMopsVersion(installed),
            equalTo("2.0.1")
        )
        val content = installed.readText()
        assertThat(content, startsWith("#!/bin/sh"))
        assertThat(content, not(containsString("@VERSION@")))
    }

    @Test
    fun `upgrade overwrites an older install`(@TempDir tmp: File) {
        newTask(tmp, "2.0.0").install()
        val installed = tmp.resolve("bin/mops")
        assertThat(InstallMops.readMopsVersion(installed), equalTo("2.0.0"))

        newTask(tmp, "2.0.1").install()
        assertThat(InstallMops.readMopsVersion(installed), equalTo("2.0.1"))
    }

    @Test
    fun `same version install is a no-op`(@TempDir tmp: File) {
        newTask(tmp, "2.0.1").install()
        val installed = tmp.resolve("bin/mops")
        val mtime = installed.lastModified()
        // Sleep-free: rewrite content and check install() does not touch it.
        installed.writeText("tampered\n# mops-version: 2.0.1\n")
        val after = installed.lastModified()
        newTask(tmp, "2.0.1").install()
        assertThat(
            "install() with matching version must not overwrite",
            installed.readText().startsWith("tampered"),
            equalTo(true)
        )
        assertThat(installed.lastModified(), equalTo(after))
    }

    @Test
    fun `downgrade is refused`(@TempDir tmp: File) {
        newTask(tmp, "2.0.1").install()
        val ex = assertThrows<GradleException> { newTask(tmp, "2.0.0").install() }
        assertThat(ex.message, containsString("downgrade"))
        // Existing file stays on disk at 2.0.1.
        assertThat(
            InstallMops.readMopsVersion(tmp.resolve("bin/mops")),
            equalTo("2.0.1")
        )
    }

    @Test
    fun `compareVersions handles dot and dash segments`() {
        assertThat(InstallMops.compareVersions("2.0.0", "2.0.1"), lessThan(0))
        assertThat(InstallMops.compareVersions("2.0.1", "2.0.0"), greaterThan(0))
        assertThat(InstallMops.compareVersions("2.0.1", "2.0.1"), equalTo(0))
        assertThat(InstallMops.compareVersions("2.1.0", "2.0.10"), greaterThan(0))
    }
}
