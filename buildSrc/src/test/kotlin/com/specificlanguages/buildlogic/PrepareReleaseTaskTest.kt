package com.specificlanguages.buildlogic

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class PrepareReleaseTaskTest {
    @TempDir
    lateinit var temporaryFolder: File

    private fun git(vararg args: String): String {
        val process = ProcessBuilder("git", *args).directory(temporaryFolder).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { output }
        return output
    }

    private fun write(path: String, content: String) {
        temporaryFolder.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun read(path: String) = temporaryFolder.resolve(path).readText()

    private fun commit() {
        git("add", ".")
        git("commit", "-m", "Fixture")
    }

    private fun wrapper(gradleVersion: String) =
        "distributionUrl=https\\://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip\n"

    private val changelogHeader = "# Changelog\n\n"

    /** A repository with `app` 1.0.0 released, depending on project `lib` and external `ext:direct`. */
    private fun releasedRepository() {
        git("init")
        git("config", "user.name", "Test")
        git("config", "user.email", "test@example.invalid")
        git("config", "commit.gpgsign", "false")
        git("config", "tag.gpgsign", "false")
        write("gradle/wrapper/gradle-wrapper.properties", wrapper("9.6.1"))
        write("subprojects/lib/gradle.properties", "version=1.0.0\n")
        write("subprojects/app/gradle.properties", "version=1.0.0\n")
        write(
            "subprojects/app/gradle.lockfile",
            "ext:direct:1.0=runtimeClasspath\next:transitive:1.0=runtimeClasspath\next:test:1.0=testRuntimeClasspath\n")
        write("subprojects/app/CHANGELOG.md", "$changelogHeader## 1.0.0\n\n- Initial.\n")
        commit()
        git("tag", "app-1.0.0")
    }

    private fun prepareRelease(version: String) {
        val projectDir = Files.createTempDirectory(temporaryFolder.toPath(), "project").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.tasks.register("prepareRelease", PrepareReleaseTask::class.java).get().apply {
            moduleName.set("app")
            currentVersion.set(version)
            changelogFile.set(temporaryFolder.resolve("subprojects/app/CHANGELOG.md"))
            propertiesFile.set(temporaryFolder.resolve("subprojects/app/gradle.properties"))
            repositoryRoot.set(temporaryFolder)
            modulePath.set("subprojects/app")
            directDependencies.set(setOf("ext:direct", "ext:test"))
            projectDependencyPaths.set(mapOf("lib" to "subprojects/lib"))
        }.prepare()
    }

    @Test
    fun `lists changed project and direct runtime dependencies and a Gradle minor update`() {
        releasedRepository()
        write("gradle/wrapper/gradle-wrapper.properties", wrapper("9.8.2"))
        write("subprojects/lib/gradle.properties", "version=1.1.0\n")
        write(
            "subprojects/app/gradle.lockfile",
            "ext:direct:2.0=runtimeClasspath\next:transitive:2.0=runtimeClasspath\next:test:2.0=testRuntimeClasspath\n")
        write("subprojects/app/gradle.properties", "version=1.0.1-SNAPSHOT\n")
        write("subprojects/app/CHANGELOG.md", "$changelogHeader## Unreleased\n\n### Dependency updates\n\n" +
            "- lib 1.1.0-SNAPSHOT, with a fix.\n\n## 1.0.0\n\n- Initial.\n")
        commit()

        prepareRelease("1.0.1-SNAPSHOT")

        assertEquals(
            "$changelogHeader## 1.0.1\n\n### Dependency updates\n\n" +
                "- lib 1.1.0, with a fix.\n" +
                "- Gradle 9.8.2. The plugin is built with this Gradle version and may not work with older ones.\n" +
                "- ext:direct 2.0\n\n## 1.0.0\n\n- Initial.\n",
            read("subprojects/app/CHANGELOG.md"))
        assertEquals("version=1.0.1\n", read("subprojects/app/gradle.properties"))
        assertEquals("", git("status", "--porcelain", "--", "subprojects"))
    }

    @Test
    fun `dependency updates alone create the release section and Gradle patch updates are not listed`() {
        releasedRepository()
        write("gradle/wrapper/gradle-wrapper.properties", wrapper("9.6.2"))
        write("subprojects/app/gradle.lockfile", "ext:direct:1.1=runtimeClasspath\n")
        write("subprojects/app/gradle.properties", "version=1.0.1-SNAPSHOT\n")
        commit()

        prepareRelease("1.0.1-SNAPSHOT")

        assertEquals(
            "$changelogHeader## 1.0.1\n\n### Dependency updates\n\n- ext:direct 1.1\n\n## 1.0.0\n\n- Initial.\n",
            read("subprojects/app/CHANGELOG.md"))
    }
}
