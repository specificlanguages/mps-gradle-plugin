package com.specificlanguages.mpsplatformcache

import org.apache.tools.ant.taskdefs.Tar
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigurationCacheTest {
    @Test
    fun `JBR extraction stores and reuses configuration cache`(@TempDir projectDir: File) {
        checkConfigurationCache(projectDir, useWindowsExtractor = false)
    }

    @Test
    fun `Windows extractor stores and reuses configuration cache`(@TempDir projectDir: File) {
        checkConfigurationCache(projectDir, useWindowsExtractor = true)
    }

    private fun checkConfigurationCache(projectDir: File, useWindowsExtractor: Boolean) {

        val content = projectDir.resolve("fixture/jbr/content.txt")
        content.parentFile.mkdirs()
        content.writeText("JBR archive content")

        val moduleDir = projectDir.resolve("repo/com/example/jbr/1.0")
        moduleDir.mkdirs()
        moduleDir.resolve("jbr-1.0.pom").writeText(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>jbr</artifactId>
                <version>1.0</version>
            </project>
            """.trimIndent()
        )
        Tar().apply {
            project = org.apache.tools.ant.Project().apply { init() }
            setBasedir(projectDir.resolve("fixture"))
            setDestFile(moduleDir.resolve("jbr-1.0.tgz"))
            setCompression(Tar.TarCompressionMethod().apply { value = "gzip" })
            execute()
        }

        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"configuration-cache-test\"")
        val rootProvider = if (useWindowsExtractor) {
            """
            providers.of(com.specificlanguages.mpsplatformcache.WindowsJbrExtraction) {
                parameters.archive.set(layout.projectDirectory.file("repo/com/example/jbr/1.0/jbr-1.0.tgz"))
                parameters.directory.set(layout.projectDirectory.dir("cache/jbr-custom/com.example/jbr/1.0"))
            }
            """.trimIndent()
        } else {
            "mpsPlatformCache.getJbrRoot(configurations.named('jbr'))"
        }
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'com.specificlanguages.mps-platform-cache'
            }

            repositories.maven { url = uri('repo') }
            configurations { jbr }
            dependencies {
                jbr 'com.example:jbr:1.0@tgz'
            }

            mpsPlatformCache.cacheRoot.set(layout.projectDirectory.dir('cache'))
            def jbrRoot = ($rootProvider).get()

            tasks.register('copyJbr', Sync) {
                from(jbrRoot)
                into(layout.buildDirectory.dir('copied-jbr'))
            }
            """.trimIndent()
        )

        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("copyJbr", "--configuration-cache", "--configuration-cache-problems=fail", "--offline")

        val first = runner.build()
        assertEquals(TaskOutcome.SUCCESS, first.task(":copyJbr")?.outcome)
        assertTrue(first.output.contains("Configuration cache entry stored."), first.output)
        assertEquals("JBR archive content", projectDir.resolve("build/copied-jbr/content.txt").readText())
        assertTrue(projectDir.resolve("cache/jbr-custom/com.example/jbr/1.0/.complete").isFile)

        val second = runner.build()
        assertTrue(second.output.contains("Reusing configuration cache."), second.output)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":copyJbr")?.outcome)

        projectDir.resolve("cache").deleteRecursively()
        val afterCacheDeletion = runner.build()
        assertTrue(afterCacheDeletion.output.contains("Reusing configuration cache."), afterCacheDeletion.output)
        assertEquals("JBR archive content", projectDir.resolve("cache/jbr-custom/com.example/jbr/1.0/content.txt").readText())
    }
}
