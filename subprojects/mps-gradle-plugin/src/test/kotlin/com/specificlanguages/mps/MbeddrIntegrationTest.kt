package com.specificlanguages.mps

import de.itemis.mps.gradle.tasks.MpsCheck
import de.itemis.mps.gradle.tasks.MpsGenerate
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MbeddrIntegrationTest {

    // The mbeddr plugin registers no tasks of its own, so each test registers the tasks whose conventions it checks.
    private fun newProject(tmp: File, mpsHome: File): Project {
        val project = ProjectBuilder.builder().withProjectDir(tmp).build()
        project.plugins.apply("com.specificlanguages.mps")
        // Prevent mps-platform-cache from trying to resolve an MPS distribution when mpsHome is queried.
        project.extensions.getByType(MpsDefaultsExtension::class.java).mpsHome.set(mpsHome)
        project.plugins.apply("de.itemis.mps.gradle.common")
        return project
    }

    private fun mpsHomeIn(tmp: File): File = tmp.resolve("mps").also { File(it, "plugins").mkdirs() }

    @Test
    fun `mpsHome defaults to mpsDefaults`(@TempDir tmp: File) {
        val mpsHome = mpsHomeIn(tmp)
        val project = newProject(tmp, mpsHome)

        val check = project.tasks.register("checkModels", MpsCheck::class.java).get()

        assertThat(check.mpsHome.get().asFile, equalTo(mpsHome))
    }

    @Test
    fun `mpsHome convention can be overridden on the task`(@TempDir tmp: File) {
        val project = newProject(tmp, mpsHomeIn(tmp))
        val otherHome = tmp.resolve("other-mps").also { it.mkdirs() }

        val check = project.tasks.register("checkModels", MpsCheck::class.java).get()
        check.mpsHome.set(otherHome)

        assertThat(check.mpsHome.get().asFile, equalTo(otherHome))
        assertThat(check.pluginRoots.get().map { it.asFile }, hasItem(File(otherHome, "plugins")))
    }

    @Test
    fun `pluginRoots includes mpsHome plugins`(@TempDir tmp: File) {
        val mpsHome = mpsHomeIn(tmp)
        val project = newProject(tmp, mpsHome)

        val check = project.tasks.register("checkModels", MpsCheck::class.java).get()

        assertThat(check.pluginRoots.get().map { it.asFile }, hasItem(File(mpsHome, "plugins")))
    }

    @Test
    fun `pluginRoots includes mpsHome plugins for tasks declaring it as a file collection`(@TempDir tmp: File) {
        val mpsHome = mpsHomeIn(tmp)
        val project = newProject(tmp, mpsHome)

        val generate = project.tasks.register("generateModels", MpsGenerate::class.java).get()

        assertThat(generate.pluginRoots.files, hasItem(File(mpsHome, "plugins")))
    }

    @Test
    fun `folderMacros are taken from pathVariables`(@TempDir tmp: File) {
        val project = newProject(tmp, mpsHomeIn(tmp))
        val macroDir = tmp.resolve("macro-dir").also { it.mkdirs() }
        project.extensions.getByType(MpsDefaultsExtension::class.java).pathVariables.put("xyz", macroDir)

        val check = project.tasks.register("checkModels", MpsCheck::class.java).get()

        assertThat(check.folderMacros.get().mapValues { it.value.asFile }, equalTo(mapOf("xyz" to macroDir)))
    }
}
