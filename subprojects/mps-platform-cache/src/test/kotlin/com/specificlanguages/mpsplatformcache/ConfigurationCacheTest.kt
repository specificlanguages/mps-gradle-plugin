package com.specificlanguages.mpsplatformcache

import org.apache.tools.ant.taskdefs.Tar
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigurationCacheTest {
    @Test
    fun `native JBR extraction stores and reuses configuration cache`(@TempDir projectDir: File) {
        assumeTrue(Os.isFamily(Os.FAMILY_UNIX), "Native tar extraction is used on Unix")

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
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps-platform-cache")
            }

            repositories.maven("repo")
            val jbr = configurations.register("jbr")
            dependencies {
                add("jbr", "com.example:jbr:1.0@tgz")
            }

            mpsPlatformCache.cacheRoot.set(layout.projectDirectory.dir("cache"))
            val jbrRoot = mpsPlatformCache.getJbrRoot(jbr).get()

            tasks.register<Sync>("copyJbr") {
                from(jbrRoot)
                into(layout.buildDirectory.dir("copied-jbr"))
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
