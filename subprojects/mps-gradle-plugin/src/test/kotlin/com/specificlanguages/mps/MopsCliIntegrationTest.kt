package com.specificlanguages.mps

import de.itemis.mps.gradle.tasks.MpsCheck
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MopsCliIntegrationTest {

    private fun newProject(tmp: File): Project {
        val project = ProjectBuilder.builder().withProjectDir(tmp).build()
        project.plugins.apply("com.specificlanguages.mps")
        project.extensions.getByType(MpsDefaultsExtension::class.java)
            .mpsHome.set(tmp.resolve("fake-mps").also { it.mkdirs() })
        return project
    }

    @Suppress("UNCHECKED_CAST")
    private fun mpsBuilds(project: Project) =
        project.extensions.getByName("mpsBuilds") as org.gradle.api.PolymorphicDomainObjectContainer<MpsBuild>

    @Test
    fun `mopsCheck is not registered without the mbeddr plugin`(@TempDir tmp: File) {
        val project = newProject(tmp)
        assertThat(project.tasks.findByName("mopsCheck"), nullValue())
    }

    @Test
    fun `mopsCheck is registered when mbeddr plugin is applied`(@TempDir tmp: File) {
        val project = newProject(tmp)
        project.plugins.apply("de.itemis.mps.gradle.common")
        val task = project.tasks.findByName("mopsCheck")
        assertThat(task, notNullValue())
        assertThat(task, instanceOf(MpsCheck::class.java))
    }

    @Test
    fun `user convention overrides mopsCheck default`(@TempDir tmp: File) {
        val project = newProject(tmp)
        val defaultsHome = tmp.resolve("mps-defaults").also { it.mkdirs() }
        val userHome = tmp.resolve("user-mps").also { it.mkdirs() }
        project.extensions.getByType(MpsDefaultsExtension::class.java).mpsHome.set(defaultsHome)

        project.plugins.apply("de.itemis.mps.gradle.common")
        project.tasks.withType(MpsCheck::class.java).configureEach {
            mpsHome.convention(project.layout.projectDirectory.dir("user-mps"))
        }

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        assertThat(mopsCheck.mpsHome.get().asFile.canonicalFile, equalTo(userHome.canonicalFile))
    }

    @Test
    fun `installMops is registered without the mbeddr plugin`(@TempDir tmp: File) {
        val project = newProject(tmp)
        val task = project.tasks.findByName("installMops")
        assertThat(task, notNullValue())
        assertThat(task, not(instanceOf(MpsCheck::class.java)))
    }

    @Test
    fun `projectLocation is auto-discovered from a single mpsBuild project directory`(@TempDir tmp: File) {
        val project = newProject(tmp)
        project.plugins.apply("de.itemis.mps.gradle.common")

        val codeDir = project.layout.projectDirectory.dir("code")
        codeDir.asFile.mkdirs()

        val builds = mpsBuilds(project)
        builds.create("main", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/main"))
            mpsProjectDirectory.set(codeDir)
        }
        builds.create("tests", TestBuild::class.java) {
            buildSolutionDescriptor.set(project.file("bar.msd"))
            mpsProjectDirectory.set(codeDir)
        }

        (project as ProjectInternal).evaluate()

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        assertThat(mopsCheck.projectLocation.get().asFile, equalTo(codeDir.asFile))
    }

    @Test
    fun `projectLocation is not auto-discovered when mpsBuilds have multiple directories`(@TempDir tmp: File) {
        val project = newProject(tmp)
        project.plugins.apply("de.itemis.mps.gradle.common")

        val dirA = project.layout.projectDirectory.dir("a")
        val dirB = project.layout.projectDirectory.dir("b")
        dirA.asFile.mkdirs()
        dirB.asFile.mkdirs()

        val builds = mpsBuilds(project)
        builds.create("a", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("a.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/a"))
            mpsProjectDirectory.set(dirA)
        }
        builds.create("b", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("b.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/b"))
            mpsProjectDirectory.set(dirB)
        }

        (project as ProjectInternal).evaluate()

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        assertThat(mopsCheck.projectLocation.get().asFile.canonicalFile, equalTo(tmp.canonicalFile))
    }

    @Test
    fun `mops projectLocation property overrides auto-discovery`(@TempDir tmp: File) {
        val project = newProject(tmp)
        val codeDir = project.layout.projectDirectory.dir("code")
        val overrideDir = project.layout.projectDirectory.dir("override")
        codeDir.asFile.mkdirs()
        overrideDir.asFile.mkdirs()

        project.extensions.extraProperties.set("mops.projectLocation", "override")
        project.plugins.apply("de.itemis.mps.gradle.common")

        mpsBuilds(project).create("main", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/main"))
            mpsProjectDirectory.set(codeDir)
        }

        (project as ProjectInternal).evaluate()

        val mopsCheck = project.tasks.named("mopsCheck", MpsCheck::class.java).get()
        assertThat(mopsCheck.projectLocation.get().asFile, equalTo(overrideDir.asFile))
    }
}
