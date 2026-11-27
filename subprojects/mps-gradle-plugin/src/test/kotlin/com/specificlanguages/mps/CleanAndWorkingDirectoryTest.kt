package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class CleanAndWorkingDirectoryTest {

    @Test
    fun `clean does not follow symbolic links or descend into subprojects`(
        @TempDir projectDir: File,
        @TempDir outsideDir: File
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "clean-test"
            include("sub", "plain")
            """.trimIndent()
        )
        val mpsPlugin = """
            plugins {
                id("com.specificlanguages.mps")
            }
        """.trimIndent()
        projectDir.resolve("build.gradle.kts").writeText(mpsPlugin)
        projectDir.resolve("sub").mkdirs()
        projectDir.resolve("sub/build.gradle.kts").writeText(mpsPlugin)
        projectDir.resolve("plain").mkdirs()

        val rootGenerated = projectDir.resolve("solutions/root/source_gen").also { it.mkdirs() }
        val subGenerated = projectDir.resolve("sub/solutions/sub/source_gen").also { it.mkdirs() }
        val plainGenerated = projectDir.resolve("plain/solutions/plain/source_gen").also { it.mkdirs() }
        val outsideGenerated = outsideDir.resolve("solutions/outside/source_gen").also { it.mkdirs() }
        Files.createSymbolicLink(projectDir.resolve("linked").toPath(), outsideDir.toPath())

        runner(projectDir, ":clean").build()

        assertFalse(rootGenerated.exists(), "$rootGenerated should be deleted")
        assertTrue(subGenerated.exists(), "$subGenerated should be kept by the root project's clean")
        assertTrue(plainGenerated.exists(), "$plainGenerated should be kept by the root project's clean")
        assertTrue(outsideGenerated.exists(), "$outsideGenerated should be kept because it is behind a symbolic link")

        runner(projectDir, ":sub:clean").build()
        assertFalse(subGenerated.exists(), "$subGenerated should be deleted by the subproject's clean")
    }

    @Test
    fun `RunAnt deletes only its default working directory`(@TempDir projectDir: File) {
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

            tasks.register<RunAnt>("defaultWorkingDirectory") {
                buildFile = layout.projectDirectory.file("build.xml")
                targets = listOf("hello")
            }

            tasks.register<RunAnt>("buildWorkingDirectory") {
                buildFile = layout.projectDirectory.file("build.xml")
                targets = listOf("hello")
                workingDirectory = layout.buildDirectory
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

        runner(projectDir, "defaultWorkingDirectory").build()

        val temporaryDir = projectDir.resolve("build/tmp/defaultWorkingDirectory")
        val workFile = temporaryDir.resolve("work/previous-run.txt").also { it.writeText("") }
        val systemFile = temporaryDir.resolve("system/cache.txt").also { it.parentFile.mkdirs(); it.writeText("") }
        val buildFile = projectDir.resolve("build/keep.txt").also { it.writeText("") }

        runner(projectDir, "defaultWorkingDirectory", "buildWorkingDirectory").build()

        assertFalse(workFile.exists(), "$workFile should be deleted with the default working directory")
        assertTrue(systemFile.exists(), "$systemFile should be kept because it is outside the working directory")
        assertTrue(buildFile.exists(), "$buildFile should be kept because a custom working directory is preserved")
    }

    private fun runner(projectDir: File, vararg tasks: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*tasks, "--configuration-cache")
}
