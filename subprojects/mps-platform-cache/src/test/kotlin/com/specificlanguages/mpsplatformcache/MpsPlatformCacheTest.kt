package com.specificlanguages.mpsplatformcache

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MpsPlatformCacheTest {
    companion object {
        private const val MPS_VERSION = "2024.3.1"
        private const val MPS_PRERELEASE_VERSION = "253.28294.10133"
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
        assertThat("MPS root should be in mps-platform-cache/mps-prerelease folder", mpsRoot, containsString("mps-platform-cache${File.separator}mps-prerelease${File.separator}$MPS_PRERELEASE_VERSION"))
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
            containsString("Expected a single dependency for configuration 'mps', found 0 dependencies")
        )
    }

    @Test
    fun `error when configuration has multiple dependencies`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            val mps by configurations.registering

            dependencies {
                mps("com.jetbrains:mps:$MPS_VERSION")
                add("mps", "com.jetbrains:mps:2024.2")
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
            containsString("Expected a single dependency for configuration 'mps'")
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
            dir.resolve("gradle.properties").writeText(
                """
                com.specificlanguages.mps-platform-cache.cacheRoot=${sharedCacheDir.absolutePath}
                """.trimIndent()
            )
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


    @Test
    fun `MPS is cached after first extraction`(@TempDir testProjectDir: File) {
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
                    val root = mpsRoot.get()
                    println("MPS root: ${'$'}root")
                    println("MPS exists: ${'$'}{root.exists()}")
                    println("MPS is directory: ${'$'}{root.isDirectory}")
                }
            }
            """.trimIndent()
        )

        // First run - should extract
        val result1 = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result1.task(":printMpsRoot")?.outcome)
        assertThat(result1.output, containsString("MPS exists: true"))
        assertThat(result1.output, containsString("MPS is directory: true"))

        // Second run - should use cached version
        val result2 = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result2.task(":printMpsRoot")?.outcome)
        assertThat(result2.output, containsString("MPS exists: true"))
        assertThat(result2.output, containsString("MPS is directory: true"))

        // Extract the MPS root path from both runs and verify they're the same
        val mpsRoot1 = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result1.output)!!.groupValues[1].trim()
        val mpsRoot2 = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result2.output)!!.groupValues[1].trim()

        assertEquals(mpsRoot1, mpsRoot2, "Both runs should use the same cached MPS root")
    }

    @Test
    fun `parallel builds share cache without conflicts`(@TempDir testProjectDir: File, @TempDir sharedCacheDir: File) {
        // Create multiple project directories that will run in parallel
        val parallelProjectDirs = (1..3).map { i ->
            testProjectDir.resolve("project$i").also { it.mkdirs() }
        }

        // Write build script to all projects
        for (dir in parallelProjectDirs) {
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
            dir.resolve("gradle.properties").writeText(
                """
                com.specificlanguages.mps-platform-cache.cacheRoot=${sharedCacheDir.absolutePath}
                """.trimIndent()
            )
        }

        // Use CountDownLatch to ensure all builds start at roughly the same time
        val startLatch = CountDownLatch(1)
        val completionLatch = CountDownLatch(parallelProjectDirs.size)
        val mpsRootsByProjectDir = mutableMapOf<File, String>()
        val errors = mutableListOf<Throwable>()

        // Launch all builds in parallel
        val threads = parallelProjectDirs.map { projectDir ->
            thread {
                try {
                    // Wait for all threads to be ready
                    startLatch.await()

                    // Run the build
                    val result = GradleRunner.create()
                        .withProjectDir(projectDir)
                        .withArguments(":printMpsRoot")
                        .withPluginClasspath()
                        .build()

                    assertEquals(TaskOutcome.SUCCESS, result.task(":printMpsRoot")?.outcome,
                        "$projectDir outcome of :printMpsRoot")

                    val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(result.output)
                    assertTrue(mpsRootMatch != null,
                        "$projectDir output should contain 'MPS root:' but was: ${result.output}")

                    val mpsRoot = mpsRootMatch!!.groupValues[1].trim()
                    synchronized(mpsRootsByProjectDir) {
                        mpsRootsByProjectDir[projectDir] = mpsRoot
                    }
                } catch (e: Throwable) {
                    synchronized(errors) {
                        errors.add(e)
                    }
                } finally {
                    completionLatch.countDown()
                }
            }
        }

        // Start all builds simultaneously
        startLatch.countDown()

        // Wait for all builds to complete (with timeout)
        val completed = completionLatch.await(5, TimeUnit.MINUTES)
        assertTrue(completed, "Not all builds completed within timeout")

        // Wait for all threads to finish
        threads.forEach { it.join() }

        // Check for errors
        if (errors.isNotEmpty()) {
            throw AssertionError("${errors.size} build(s) failed. First error: ${errors.first().message}", errors.first())
        }

        // Verify all builds succeeded and used the same MPS root
        assertEquals(parallelProjectDirs.size, mpsRootsByProjectDir.size, "All builds should have completed successfully")

        val uniqueRoots = mpsRootsByProjectDir.values.distinct()
        assertEquals(1, uniqueRoots.size,
            "All parallel builds should use the same MPS root, but had: $mpsRootsByProjectDir")
    }

    @Test
    fun `recovery from failed extraction cleans up and re-extracts`(@TempDir testProjectDir: File) {
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
                    val root = mpsRoot.get()
                    println("MPS root: ${'$'}root")
                    println("MPS exists: ${'$'}{root.exists()}")
                }
            }
            """.trimIndent()
        )

        // First, run a build to find out where the cache directory will be
        val initialResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, initialResult.task(":printMpsRoot")?.outcome)

        // Extract the MPS root path
        val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(initialResult.output)
        assertTrue(mpsRootMatch != null, "output should contain 'MPS root:' but was: ${initialResult.output}")
        val mpsRootPath = mpsRootMatch!!.groupValues[1].trim()
        val mpsRoot = File(mpsRootPath)

        // Simulate a failed extraction by removing the .complete marker
        // This simulates a scenario where extraction started but didn't finish
        val completeMarker = File(mpsRoot, ".complete")
        assertTrue(completeMarker.exists(), ".complete marker should exist after successful extraction")
        assertTrue(completeMarker.delete(), "Should be able to delete .complete marker")

        // Also add a dummy file to simulate partial extraction content
        val dummyFile = File(mpsRoot, "partial-extraction-marker.txt")
        dummyFile.writeText("This simulates a partially extracted file")

        // Verify the incomplete state
        assertFalse(completeMarker.exists(), ".complete marker should be deleted")
        assertTrue(dummyFile.exists(), "Dummy file should exist")

        // Run the build again - it should detect the incomplete extraction, clean up, and re-extract
        val recoveryResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, recoveryResult.task(":printMpsRoot")?.outcome,
            "Build should succeed after recovering from failed extraction")
        assertThat(recoveryResult.output, containsString("MPS exists: true"))

        // Verify the recovery: .complete marker should exist again
        assertTrue(completeMarker.exists(), ".complete marker should be recreated after recovery")

        // Verify the dummy file was cleaned up (the directory was deleted and recreated)
        assertFalse(dummyFile.exists(), "Partial extraction marker should be cleaned up")

        // Verify the extracted MPS is valid (should be a directory with content)
        assertTrue(mpsRoot.exists(), "MPS root should exist")
        assertTrue(mpsRoot.isDirectory, "MPS root should be a directory")
        val filesInRoot = mpsRoot.listFiles()?.filter { it.name != ".complete" } ?: emptyList()
        assertTrue(filesInRoot.isNotEmpty(), "MPS root should contain files other than .complete marker")
    }

    @Test
    fun `extraction with stale lock file proceeds correctly`(@TempDir testProjectDir: File) {
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
                    println("MPS exists: ${'$'}{mpsRoot.get().exists()}")
                }
            }
            """.trimIndent()
        )

        // Run a build first to discover the cache location
        val initialResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, initialResult.task(":printMpsRoot")?.outcome)
        val mpsRootMatch = Regex("^MPS root:(.*)$", RegexOption.MULTILINE).find(initialResult.output)
        assertTrue(mpsRootMatch != null)
        val mpsRootPath = mpsRootMatch!!.groupValues[1].trim()
        val mpsRoot = File(mpsRootPath)

        // Simulate a previous process that crashed: delete the extraction but leave a stale lock file
        val completeMarker = MpsPlatformCache.getCompletionFileForDistributionDir(mpsRoot)
        Files.delete(completeMarker.toPath())

        // Delete the entire MPS directory to simulate complete failure
        mpsRoot.deleteRecursively()
        assertFalse(mpsRoot.exists(), "MPS directory should not exist after deletion")

        // Create a stale lock file (this simulates a crash scenario)
        val lockFile = MpsPlatformCache.getLockFileForDistributionDir(mpsRoot)
        lockFile.createNewFile()
        assertTrue(lockFile.exists(), "Lock file should exist for test setup")

        // Now run the build again - it should be able to acquire the lock and extract successfully
        val recoveryResult = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printMpsRoot")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, recoveryResult.task(":printMpsRoot")?.outcome,
            "Build should succeed even with stale lock file present")
        assertThat(recoveryResult.output, containsString("MPS exists: true"))

        // Verify extraction completed successfully
        assertTrue(mpsRoot.exists(), "MPS root should exist after recovery")
        assertTrue(mpsRoot.isDirectory, "MPS root should be a directory")
        assertTrue(completeMarker.exists(), ".complete marker should exist")

        val filesInRoot = mpsRoot.listFiles()?.filter { it.absoluteFile != completeMarker.absoluteFile } ?: emptyList()
        assertTrue(filesInRoot.isNotEmpty(), "MPS root should contain extracted files")
    }
}
