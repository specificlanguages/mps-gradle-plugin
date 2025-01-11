package com.specificlanguages.jbrtoolchain

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JbrToolchainTest {
    companion object {
        private const val JBR_BUILD = "11.0.10+9-b1341.41"
        private const val JBR_VERSION = "11_0_10-b1341.41"
    }

    @Test
    fun canRunJava(@TempDir testProjectDir: File) {
        val settingsFile = testProjectDir.resolve("settings.gradle.kts")
        val buildFile = testProjectDir.resolve("build.gradle.kts")

        settingsFile.writeText("")

        buildFile.writeText("""
            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }
            
            dependencies {
                jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
            }
            
            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val javaVersion by tasks.registering(JavaExec::class) {
                javaLauncher = jbrToolchain.javaLauncher
                jvmArgs("-version")
                mainClass = "dummy"
            }
        """.trimIndent())

        val task = ":javaVersion"
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(task)
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
        assertTrue(result.output.contains(JBR_BUILD)) {
            "output should contain JBR version $JBR_BUILD but was: ${result.output}"
        }
    }

    @Test
    fun projectsShareJbr(@TempDir project1Dir: File, @TempDir project2Dir: File) {
        val projectDirs = listOf(project1Dir, project2Dir)

        for (dir in projectDirs) {
            dir.resolve("settings.gradle.kts").writeText("")
            dir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.specificlanguages.jbr-toolchain")
                }
                
                dependencies {
                    jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
                }
                
                repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")
    
                val printJavaHome by tasks.registering {
                    doLast {
                        val javaHome = jbrToolchain.javaLauncher.get().metadata.installationPath
                        println("Java home: ${'$'}javaHome")
                
                    }
                }
            """.trimIndent()
            )
        }

        val javaHomes = projectDirs.map { projectDir ->
            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(":printJavaHome")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":printJavaHome")?.outcome, "$projectDir outcome of :printJavaHome")

            val javaHomeMatch = Regex("^Java home:(.*)$", RegexOption.MULTILINE).find(result.output)
            assertTrue(javaHomeMatch != null, "$projectDir output should contain 'Java home:' but was: ${result.output}")

            javaHomeMatch!!.groupValues[1]
        }

        assertEquals(javaHomes[0], javaHomes[1], "Java home should be the same in both projects")
    }

}