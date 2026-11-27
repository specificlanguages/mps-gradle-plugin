package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class CleanAndWorkingDirectoryTest {

    @Test
    fun `cleanMpsGenerated searches only the MPS project directories`(
        @TempDir projectDir: File,
        @TempDir outsideDir: File
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "clean-test"
            include("sub", "plain")
            project(":sub").projectDir = file("code/sub")
            project(":plain").projectDir = file("code/plain")
            """.trimIndent()
        )
        fun mpsProject(mpsProjectDirectory: String) = """
            import com.specificlanguages.mps.MainBuild

            plugins {
                id("com.specificlanguages.mps")
            }

            mpsBuilds {
                create<MainBuild>("main") {
                    mpsProjectDirectory = layout.projectDirectory.dir("$mpsProjectDirectory")
                }
            }
        """.trimIndent()
        projectDir.resolve("build.gradle.kts").writeText(
            mpsProject("code") + "\n\nlayout.buildDirectory = layout.projectDirectory.dir(\"code/build\")\n"
        )
        projectDir.resolve("code/sub").mkdirs()
        projectDir.resolve("code/sub/build.gradle.kts").writeText(mpsProject("."))
        projectDir.resolve("code/plain").mkdirs()

        fun generatedDir(path: String) = projectDir.resolve(path).also { it.mkdirs() }

        val rootGenerated = generatedDir("code/solutions/root/source_gen")
        val outsideMpsProject = generatedDir("other/solutions/other/source_gen")
        val hidden = generatedDir("code/.hidden/source_gen")
        val inBuildDirectory = generatedDir("code/build/unpacked/source_gen")
        val subGenerated = generatedDir("code/sub/solutions/sub/source_gen")
        val plainGenerated = generatedDir("code/plain/solutions/plain/source_gen")
        val outsideGenerated = outsideDir.resolve("solutions/outside/source_gen").also { it.mkdirs() }
        Files.createSymbolicLink(projectDir.resolve("code/linked").toPath(), outsideDir.toPath())

        runner(projectDir, ":cleanMpsGenerated").build()

        assertFalse(rootGenerated.exists(), "$rootGenerated should be deleted")
        assertTrue(outsideMpsProject.exists(), "$outsideMpsProject should be kept, it is outside the MPS project")
        assertTrue(hidden.exists(), "$hidden should be kept, it is in a hidden directory")
        assertTrue(inBuildDirectory.exists(), "$inBuildDirectory should be kept, it is in the build directory")
        assertTrue(subGenerated.exists(), "$subGenerated should be kept, it belongs to a subproject")
        assertTrue(plainGenerated.exists(), "$plainGenerated should be kept, it belongs to a subproject")
        assertTrue(outsideGenerated.exists(), "$outsideGenerated should be kept, it is behind a symbolic link")

        val result = runner(projectDir, ":sub:clean").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":sub:cleanMpsGenerated")?.outcome)
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
