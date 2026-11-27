package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigurationCacheTest {

    @Test
    fun `clean deletes the generated directories and keeps the configuration cache reusable`(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "clean-test"""")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps")
            }
            """.trimIndent()
        )

        val generatedDirs = listOf("source_gen", "source_gen.caches", "classes_gen", "test_gen", "test_gen.caches")
            .map { projectDir.resolve("solutions/sample/$it") }
        val source = projectDir.resolve("solutions/sample/models/sample.mps")

        fun createGeneratedDirs() {
            generatedDirs.forEach { it.mkdirs(); it.resolve("Generated.java").writeText("") }
        }

        source.parentFile.mkdirs()
        source.writeText("")
        createGeneratedDirs()

        val first = runner(projectDir, "clean").build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":clean")?.outcome)
        generatedDirs.forEach { assertFalse(it.exists(), "$it should be deleted") }
        assertTrue(source.exists(), "$source should be kept")

        createGeneratedDirs()

        val second = runner(projectDir, "clean").build()
        assertThat(second.output, containsString("Reusing configuration cache."))
        generatedDirs.forEach { assertFalse(it.exists(), "$it should be deleted") }
    }

    @Test
    fun `RunAnt runs after clean in the same build`(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "run-ant-test"

            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import com.specificlanguages.mps.RunAnt

            plugins {
                `java-base`
                id("com.specificlanguages.mps")
            }

            val antLibraries = configurations.create("antLibraries")

            dependencies {
                antLibraries("org.apache.ant:ant-launcher:1.10.15")
                antLibraries("org.apache.ant:ant:1.10.15")
            }

            mpsDefaults {
                mpsHome = layout.projectDirectory.dir("fake-mps")
                javaLauncher = javaToolchains.launcherFor { }
                antClasspath.from(antLibraries)
            }

            tasks.register<RunAnt>("runAnt") {
                buildFile = layout.projectDirectory.file("build.xml")
                targets = listOf("hello")
            }
            """.trimIndent()
        )
        projectDir.resolve("build.xml").writeText(
            """
            <project name="hello">
                <target name="hello">
                    <echo message="Hello from Ant"/>
                </target>
            </project>
            """.trimIndent()
        )

        // The first build creates the build directory, so that `clean` in the second one has something to delete.
        runner(projectDir, "runAnt").build()

        val result = runner(projectDir, "clean", "runAnt").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":runAnt")?.outcome)
        assertThat(result.output, containsString("Hello from Ant"))
    }

    private fun runner(projectDir: File, vararg tasks: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*tasks, "--configuration-cache")
}
