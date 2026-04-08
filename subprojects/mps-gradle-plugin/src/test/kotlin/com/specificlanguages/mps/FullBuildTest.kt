package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class FullBuildTest {

    companion object {
        private val TEST_PROJECT_DIR = File("etc/test-projects/mps-json")

        /**
         * The cache root shared with the outer Gradle build. The mps-platform-cache plugin in the test project
         * will use this directory to avoid re-downloading MPS for every test run.
         */
        private val CACHE_ROOT = File(System.getProperty("test.mps-platform-cache.cacheRoot",
            File("../../build/mps-platform-cache").absolutePath))
    }

    @Test
    fun `full build produces ZIP with expected jars`(@TempDir workDir: File) {
        TEST_PROJECT_DIR.copyRecursively(workDir)
        writeGradleProperties(workDir)

        val result = GradleRunner.create()
            .withProjectDir(workDir)
            .withPluginClasspath()
            .withArguments("assemble")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateBuildScripts")?.outcome,
            "generateBuildScripts should succeed")
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMain")?.outcome,
            "generate task should succeed")
        assertEquals(TaskOutcome.SUCCESS, result.task(":assembleMain")?.outcome,
            "assemble task should succeed")
        assertEquals(TaskOutcome.SUCCESS, result.task(":zip")?.outcome,
            "zip task should succeed")

        val zipFile = workDir.resolve("build/distributions/mps-json-1.0.0-test.zip")
        assertThat("ZIP file should be produced", zipFile.exists(), `is`(true))

        val jarNames = ZipFile(zipFile).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.endsWith(".jar") }
                .map { it.substringAfterLast("/") }
                .toSet()
        }

        assertThat("ZIP should contain the language jar",
            jarNames, hasItem(containsString("com.specificlanguages.json")))
    }

    private fun writeGradleProperties(projectDir: File) {
        projectDir.resolve("gradle.properties").writeText(
            "com.specificlanguages.mps-platform-cache.cacheRoot=${CACHE_ROOT.absolutePath}\n"
        )
    }
}
