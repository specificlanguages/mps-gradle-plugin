package com.specificlanguages.jbrtoolchain

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JbrCachingTest {
    companion object {
        private const val JBR_VERSION = "17.0.6-b469.82"
    }

    @Test
    fun `getJbrRoot caches official JBR jbr_jcef`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }

            dependencies {
                jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printJbrRoot by tasks.registering {
                val jbrRoot = mpsPlatformCache.getJbrRoot(configurations.jbr)

                doLast {
                    println("JBR root: ${'$'}{jbrRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printJbrRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printJbrRoot")?.outcome)

        val jbrRootMatch = Regex("^JBR root:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertNotNull(jbrRootMatch, "output should contain 'JBR root:' but was: ${result.output}")

        val jbrRoot = jbrRootMatch!!.groupValues[1].trim()
        assertThat("JBR root should be in mps-platform-cache/jbr_jcef folder", jbrRoot, containsString("mps-platform-cache${File.separator}jbr_jcef${File.separator}$JBR_VERSION"))
        assertTrue(File(jbrRoot).isDirectory, "JBR root should be a directory: $jbrRoot")
    }

    @Test
    fun `getJbrRoot caches official JBR jbr`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }

            dependencies {
                jbr("com.jetbrains.jdk:jbr:$JBR_VERSION")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printJbrRoot by tasks.registering {
                val jbrRoot = mpsPlatformCache.getJbrRoot(configurations.jbr)

                doLast {
                    println("JBR root: ${'$'}{jbrRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printJbrRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printJbrRoot")?.outcome)

        val jbrRootMatch = Regex("^JBR root:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertTrue(jbrRootMatch != null, "output should contain 'JBR root:' but was: ${result.output}")

        val jbrRoot = jbrRootMatch!!.groupValues[1].trim()
        assertThat("JBR root should be in mps-platform-cache/jbr folder", jbrRoot, containsString("mps-platform-cache${File.separator}jbr${File.separator}$JBR_VERSION"))
    }

    @Test
    fun `projects share same JBR cache`(@TempDir project1Dir: File, @TempDir project2Dir: File, @TempDir sharedCacheDir: File) {
        val projectDirs = listOf(project1Dir, project2Dir)

        for (dir in projectDirs) {
            dir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.specificlanguages.jbr-toolchain")
                }

                dependencies {
                    jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
                }

                repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

                val printJbrRoot by tasks.registering {
                    val jbrRoot = mpsPlatformCache.getJbrRoot(configurations.jbr)

                    doLast {
                        println("JBR root: ${'$'}{jbrRoot.get()}")
                    }
                }
                """.trimIndent()
            )
            dir.resolve("gradle.properties").writeText(
                """
                com.specificlanguages.mps-platform-cache.cacheRoot=${sharedCacheDir.absolutePath}
                """.trimIndent()
            )
        }

        val jbrRoots = projectDirs.map { projectDir ->
            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(":printJbrRoot")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":printJbrRoot")?.outcome, "$projectDir outcome of :printJbrRoot")

            val jbrRootMatch = Regex("^JBR root:(.*)$", RegexOption.MULTILINE).find(result.output)
            assertTrue(jbrRootMatch != null, "$projectDir output should contain 'JBR root:' but was: ${result.output}")

            jbrRootMatch!!.groupValues[1].trim()
        }

        assertEquals(jbrRoots[0], jbrRoots[1], "JBR root should be the same in both projects")
    }
}
