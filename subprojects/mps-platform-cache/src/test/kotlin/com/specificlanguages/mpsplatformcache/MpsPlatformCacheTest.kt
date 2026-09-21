package com.specificlanguages.mpsplatformcache

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

class MpsPlatformCacheTest {
    /**
     * Points the project at [cacheRoot]. Writes the property through [Properties] rather than as plain text: a
     * Windows path contains backslashes, which are escape characters in a properties file and are dropped when
     * read back.
     */
    private fun writeCacheRootProperty(projectDir: File, cacheRoot: File) {
        projectDir.resolve("gradle.properties").outputStream().use {
            val gradleProperties = Properties()
            gradleProperties["com.specificlanguages.mps-platform-cache.cacheRoot"] = cacheRoot.absolutePath
            gradleProperties.store(it, null)
        }
    }

    companion object {
        private const val MPS_VERSION = "2024.3.1"

        /**
         * Prereleases are removed from the repository after some time, so the tests resolve the latest one that is
         * available rather than a fixed version.
         */
        private const val MPS_PRERELEASE_VERSION = "+"
    }

    @Test
    fun `getMpsRoot caches official JetBrains MPS`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.jetbrains:mps:$MPS_VERSION")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                doLast {
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMpsRoot")?.outcome)

        val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertTrue(mpsRootMatch != null, "output should contain 'MPS root:' but was: ${result.output}")

        val mpsRoot = mpsRootMatch!!.groupValues[1].trim()
        assertThat("MPS root should be in mps-platform-cache/mps folder", mpsRoot, containsString("mps-platform-cache${File.separator}mps${File.separator}$MPS_VERSION"))
        assertTrue(File(mpsRoot).isDirectory, "MPS root should be a directory: $mpsRoot")
        assertTrue(File(mpsRoot, "lib/mps-core.jar").isFile, "MPS archive content should be extracted")
        assertTrue(File(mpsRoot, ".complete").isFile, "Extraction should be complete")
    }

    @Test
    fun `getMpsRoot caches MPS prerelease`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.jetbrains.mps:mps-prerelease:$MPS_PRERELEASE_VERSION")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)
                val mpsVersion = mps.map { it.resolvedConfiguration.resolvedArtifacts.single().moduleVersion.id.version }

                doLast {
                    println("MPS version: ${'$'}{mpsVersion.get()}")
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMpsRoot")?.outcome)

        val mpsVersionMatch = Regex("^MPS version:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertTrue(mpsVersionMatch != null, "output should contain 'MPS version:' but was: ${result.output}")
        val mpsVersion = mpsVersionMatch!!.groupValues[1].trim()

        val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertTrue(mpsRootMatch != null, "output should contain 'MPS root:' but was: ${result.output}")

        val mpsRoot = mpsRootMatch!!.groupValues[1].trim()
        assertThat("MPS root should be in mps-platform-cache/mps-prerelease folder", mpsRoot, containsString("mps-platform-cache${File.separator}mps-prerelease${File.separator}$mpsVersion"))
    }

    @Test
    fun `getMpsRoot caches MPS reached transitively`(@TempDir testProjectDir: File, @TempDir markerRepoDir: File) {
        val markerVersion = "1.0-test"
        val markerPom = markerRepoDir.resolve("com/example/mps-marker/$markerVersion/mps-marker-$markerVersion.pom")
        markerPom.parentFile.mkdirs()
        markerPom.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>mps-marker</artifactId>
              <version>$markerVersion</version>
              <packaging>pom</packaging>
              <dependencies>
                <dependency>
                  <groupId>com.jetbrains</groupId>
                  <artifactId>mps</artifactId>
                  <version>$MPS_VERSION</version>
                  <type>zip</type>
                </dependency>
              </dependencies>
            </project>
            """.trimIndent()
        )

        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.example:mps-marker:$markerVersion")
            }

            repositories {
                maven("${markerRepoDir.toURI()}")
                maven("https://artifacts.itemis.cloud/repository/maven-mps")
            }

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                doLast {
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printMpsRoot")?.outcome)

        val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result.output)
        assertTrue(mpsRootMatch != null, "output should contain 'MPS root:' but was: ${result.output}")

        val mpsRoot = mpsRootMatch!!.groupValues[1].trim()
        assertThat(
            "MPS reached transitively should be cached under its own coordinates",
            mpsRoot,
            containsString("mps-platform-cache${File.separator}mps${File.separator}$MPS_VERSION")
        )
        assertTrue(File(mpsRoot).isDirectory, "MPS root should be a directory: $mpsRoot")
    }


    @Test
    fun `error when configuration has no dependencies`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                doLast {
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":printMpsRoot")?.outcome)
        assertThat(
            result.output,
            containsString("Expected configuration 'mps' to resolve to a single artifact, but it resolved to 0 artifacts")
        )
    }

    @Test
    fun `error when configuration resolves to multiple artifacts`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.jetbrains:mps:$MPS_VERSION")
                add("mps", "com.jetbrains.mps:mps-prerelease:$MPS_PRERELEASE_VERSION")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                doLast {
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":printMpsRoot")?.outcome)
        assertThat(
            result.output,
            containsString("Expected configuration 'mps' to resolve to a single artifact, but it resolved to 2 artifacts")
        )
    }

    @Test
    fun `error when configuration has unresolved dependency`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.nonexistent:artifact:1.0.0")
            }

            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val printMpsRoot by tasks.registering {
                val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                doLast {
                    println("MPS root: ${'$'}{mpsRoot.get()}")
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            // The wrapping exception with the text we are looking for is only reported when the full stack trace is active.
            .withArguments(":printMpsRoot", "--stacktrace")
            .withPluginClasspath()
            .buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":printMpsRoot")?.outcome)
        assertThat(
            result.output,
            containsString("Could not resolve configuration 'mps'")
        )
    }

    @Test
    fun `projects share same MPS cache`(@TempDir project1Dir: File, @TempDir project2Dir: File, @TempDir sharedCacheDir: File) {
        val projectDirs = listOf(project1Dir, project2Dir)

        for (dir in projectDirs) {
            dir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                    id("com.specificlanguages.mps-platform-cache")
                }

                val mps by configurations.registering

                dependencies {
                    mps("com.jetbrains:mps:$MPS_VERSION")
                }

                repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

                val printMpsRoot by tasks.registering {
                    val mpsRoot = mpsPlatformCache.getMpsRoot(mps)

                    doLast {
                        println("MPS root: ${'$'}{mpsRoot.get()}")
                    }
                }
                """.trimIndent()
            )
            writeCacheRootProperty(dir, sharedCacheDir)
        }

        val mpsRoots = projectDirs.map { projectDir ->
            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(":printMpsRoot")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":printMpsRoot")?.outcome, "$projectDir outcome of :printMpsRoot")

            val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result.output)
            assertTrue(mpsRootMatch != null, "$projectDir output should contain 'MPS root:' but was: ${result.output}")

            mpsRootMatch!!.groupValues[1].trim()
        }

        assertEquals(mpsRoots[0], mpsRoots[1], "MPS root should be the same in both projects")
    }
}
