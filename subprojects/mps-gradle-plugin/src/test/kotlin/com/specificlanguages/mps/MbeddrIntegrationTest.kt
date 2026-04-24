package com.specificlanguages.mps

import de.itemis.mps.gradle.tasks.MpsCheck
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MbeddrIntegrationTest {

    private fun newProject(tmp: File): Project {
        val project = ProjectBuilder.builder().withProjectDir(tmp).build()
        project.plugins.apply("com.specificlanguages.mps")
        // Prevent mps-platform-cache from trying to resolve an MPS distribution when mpsHome is queried.
        project.extensions.getByType(MpsDefaultsExtension::class.java)
            .mpsHome.set(tmp.resolve("fake-mps").also { it.mkdirs() })
        return project
    }

    @Test
    fun `mpsHome defaults to mpsDefaults`(@TempDir tmp: File) {
        val project = newProject(tmp)
        val defaultsHome = tmp.resolve("mps-defaults").also { it.mkdirs() }
        project.extensions.getByType(MpsDefaultsExtension::class.java).mpsHome.set(defaultsHome)

        project.plugins.apply("de.itemis.mps.gradle.common")

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        assertThat(mopsCheck.mpsHome.get().asFile, equalTo(defaultsHome))
    }

    @Test
    fun `pluginRoots includes mpsHome plugins`(@TempDir tmp: File) {
        val project = newProject(tmp)
        val mpsHomeDir = tmp.resolve("mps").also { it.mkdirs() }
        File(mpsHomeDir, "plugins").mkdirs()

        project.extensions.getByType(MpsDefaultsExtension::class.java).mpsHome.set(mpsHomeDir)

        project.plugins.apply("de.itemis.mps.gradle.common")

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        val roots = mopsCheck.pluginRoots.get().map { it.asFile }
        assertThat(roots, hasItem(File(mpsHomeDir, "plugins")))
    }
}
