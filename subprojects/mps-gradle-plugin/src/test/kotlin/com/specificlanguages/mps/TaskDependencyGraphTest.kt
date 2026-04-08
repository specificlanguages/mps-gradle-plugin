package com.specificlanguages.mps

import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@Suppress("UNCHECKED_CAST")
class TaskDependencyGraphTest {

    private lateinit var project: ProjectInternal

    @BeforeEach
    fun setUp(@TempDir tempDir: Path) {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build() as ProjectInternal
        project.plugins.apply("com.specificlanguages.mps")

        // Override mpsHome to a dummy directory so that the mps-platform-cache provider chain
        // (which requires an mps dependency) is not triggered when resolving task dependencies.
        val mpsDefaults = project.extensions.getByType(MpsDefaultsExtension::class.java)
        mpsDefaults.mpsHome.set(tempDir.resolve("fake-mps").toFile())
    }

    private val mpsBuilds: org.gradle.api.PolymorphicDomainObjectContainer<MpsBuild>
        get() = project.extensions.getByName("mpsBuilds")
            as org.gradle.api.PolymorphicDomainObjectContainer<MpsBuild>

    private val bundledDependencies: org.gradle.api.NamedDomainObjectContainer<BundledDependency>
        get() = project.extensions.getByName("bundledDependencies")
            as org.gradle.api.NamedDomainObjectContainer<BundledDependency>

    @Test
    fun `single MainBuild wires generate, assemble, zip, and assemble lifecycle`() {
        mpsBuilds.create("main", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/main"))
        }

        project.evaluate()

        assertTaskDependsOn("generateMain", "generateBuildScripts")
        assertTaskDependsOn("assembleMain", "generateMain")
        assertTaskDependsOn("zip", "assembleMain")
        assertTaskDependsOn("assemble", "zip")
    }

    @Test
    fun `single TestBuild wires generate, check, test, and check lifecycle`() {
        mpsBuilds.create("myTest", TestBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
        }

        project.evaluate()

        assertTaskDependsOn("generateMyTest", "generateBuildScripts")
        assertTaskDependsOn("checkMyTest", "generateMyTest")
        assertTaskDependsOn("test", "checkMyTest")
        assertTaskDependsOn("check", "test")
    }

    @Test
    fun `MainBuild and TestBuild wire correctly together`() {
        val main = mpsBuilds.create("main", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/main"))
        }

        mpsBuilds.create("tests", TestBuild::class.java) {
            dependsOn(main)
            buildSolutionDescriptor.set(project.file("bar.msd"))
        }

        project.evaluate()

        // MainBuild wiring
        assertTaskDependsOn("generateMain", "generateBuildScripts")
        assertTaskDependsOn("assembleMain", "generateMain")
        assertTaskDependsOn("zip", "assembleMain")
        assertTaskDependsOn("assemble", "zip")

        // TestBuild wiring
        assertTaskDependsOn("generateTests", "generateBuildScripts")
        assertTaskDependsOn("checkTests", "generateTests")
        assertTaskDependsOn("test", "checkTests")
        assertTaskDependsOn("check", "test")

        // Cross-build dependency: TestBuild's generate depends on MainBuild's assemble
        assertTaskDependsOn("generateTests", "assembleMain")
    }

    @Test
    fun `multiple MainBuilds with dependsOn wire generate to assemble of dependency`() {
        val buildA = mpsBuilds.create("buildA", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("a.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/buildA"))
        }

        mpsBuilds.create("buildB", MainBuild::class.java) {
            dependsOn(buildA)
            buildSolutionDescriptor.set(project.file("b.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/buildB"))
        }

        project.evaluate()

        // Build A wiring
        assertTaskDependsOn("generateBuildA", "generateBuildScripts")
        assertTaskDependsOn("assembleBuildA", "generateBuildA")

        // Build B wiring
        assertTaskDependsOn("generateBuildB", "generateBuildScripts")
        assertTaskDependsOn("assembleBuildB", "generateBuildB")

        // Cross-build dependency: B's generate depends on A's assemble
        assertTaskDependsOn("generateBuildB", "assembleBuildA")

        // Both contribute to zip
        assertTaskDependsOn("zip", "assembleBuildA")
        assertTaskDependsOn("zip", "assembleBuildB")
    }

    @Test
    fun `bundledDependencies resolve tasks wire before generateBuildScripts`() {
        mpsBuilds.create("main", MainBuild::class.java) {
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/main"))
        }

        bundledDependencies.create("myLib") {
            destinationDir.set(project.layout.projectDirectory.dir("libs"))
        }

        project.evaluate()

        // The resolve task for the bundled dependency should be wired before generateBuildScripts
        // via the setup task chain: resolveMyLib -> setup -> generateBuildScripts
        assertTaskDependsOn("setup", "resolveMyLib")
        assertTaskDependsOn("generateBuildScripts", "setup")
    }

    @Test
    fun `unpublished MainBuild does not contribute to zip`() {
        mpsBuilds.create("internal", MainBuild::class.java) {
            published.set(false)
            buildSolutionDescriptor.set(project.file("foo.msd"))
            buildArtifactsDirectory.set(project.layout.buildDirectory.dir("artifacts/internal"))
        }

        project.evaluate()

        // The zip task should still exist but not depend on assembleInternal
        val zipDeps = taskDependencyNames("zip")
        assertThat(zipDeps, not(hasItem("assembleInternal")))
    }

    private fun taskDependencyNames(taskName: String): Set<String> {
        val task = project.tasks.getByName(taskName)
        return task.taskDependencies.getDependencies(task).map(Task::getName).toSet()
    }

    private fun assertTaskDependsOn(taskName: String, dependencyName: String) {
        assertThat(
            "Task ':$taskName' should depend on ':$dependencyName'",
            taskDependencyNames(taskName),
            hasItem(dependencyName)
        )
    }
}
