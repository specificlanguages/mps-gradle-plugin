package com.specificlanguages.buildlogic

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class CheckReleaseVersionsTaskTest {
    @TempDir
    lateinit var temporaryFolder: File

    private fun module(name: String, vararg dependencies: String) =
        ModuleInfo(name, "1.0.0", "subprojects/$name", dependencies.toSet())

    private val chain = listOf(
        module("library"), module("middle", "library"), module("consumer", "middle"), module("unrelated")
    )

    private fun git(vararg args: String) {
        val process = ProcessBuilder("git", *args).directory(temporaryFolder).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { output }
    }

    private fun write(path: String, content: String) {
        temporaryFolder.resolve(path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun commit() {
        git("add", ".")
        git("commit", "-m", "Fixture")
    }

    private fun releasedRepository(modules: List<ModuleInfo> = chain) {
        git("init")
        git("config", "user.name", "Test")
        git("config", "user.email", "test@example.invalid")
        git("config", "commit.gpgsign", "false")
        git("config", "tag.gpgsign", "false")
        modules.forEach {
            write("${it.path}/src/main/example.txt", "source")
            write("${it.path}/gradle.lockfile", "example:dependency:1.0=runtimeClasspath\n")
        }
        commit()
        modules.forEach { git("tag", "${it.name}-1.0.0") }
    }

    private fun checkVersions(versions: List<ModuleInfo> = chain) {
        val projectDir = Files.createTempDirectory(temporaryFolder.toPath(), "project").toFile()
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.tasks.register("checkReleaseVersions", CheckReleaseVersionsTask::class.java).get().apply {
            repositoryRoot.set(temporaryFolder)
            modules.set(versions)
        }.check()
    }

    private fun assertRequiredBumps(expected: Set<String>, modules: List<ModuleInfo> = chain) {
        val message = assertThrows<GradleException> { checkVersions(modules) }.message!!
        val affected = Regex("(?:module|dependent) '([^']+)' must be bumped").findAll(message)
            .map { it.groupValues[1] }.toList()
        assertEquals(expected, affected.toSet(), message)
        assertEquals(expected.size, affected.size, message)
    }

    @Test
    fun `source change requires the module itself to be bumped`() {
        releasedRepository()
        write("subprojects/unrelated/src/main/example.txt", "changed")
        commit()
        assertRequiredBumps(setOf("unrelated"))
    }

    @Test
    fun `runtime lock change reaches all transitive dependents`() {
        releasedRepository()
        write("subprojects/library/gradle.lockfile", "example:dependency:1.1=runtimeClasspath\n")
        commit()
        assertRequiredBumps(setOf("library", "middle", "consumer"))
    }

    @Test
    fun `changes propagate to dependents but not dependencies`() {
        releasedRepository()
        write("subprojects/middle/src/main/example.txt", "changed")
        commit()
        assertRequiredBumps(setOf("middle", "consumer"))
    }

    @Test
    fun `diamond reaches both branches and their shared dependent once`() {
        val modules = listOf(
            module("library"), module("left", "library"), module("right", "library"),
            module("consumer", "left", "right"), module("unrelated")
        )
        releasedRepository(modules)
        write("subprojects/library/src/main/example.txt", "changed")
        commit()
        assertRequiredBumps(setOf("library", "left", "right", "consumer"), modules)
    }

    @Test
    @Timeout(10)
    fun `cycles and self dependencies terminate and include each affected module once`() {
        val modules = listOf(
            module("library", "library", "middle"), module("middle", "library"),
            module("consumer", "middle"), module("unrelated")
        )
        releasedRepository(modules)
        write("subprojects/library/src/main/example.txt", "changed")
        commit()
        assertRequiredBumps(setOf("library", "middle", "consumer"), modules)
    }

    @Test
    fun `bumps satisfy the policy`() {
        releasedRepository()
        write("subprojects/library/gradle.lockfile", "example:dependency:1.1=runtimeClasspath\n")
        commit()
        checkVersions(chain.map { if (it.name == "unrelated") it else it.copy(version = "1.0.1-SNAPSHOT") })
    }

    @Test
    fun `unchanged releases need no bumps`() {
        releasedRepository()
        checkVersions()
    }

    @Test
    fun `tests fixtures build scripts and Markdown need no bumps`() {
        releasedRepository()
        write("subprojects/library/src/test/Test.kt", "test")
        write("subprojects/library/etc/test-projects/fixture/build.gradle.kts", "fixture")
        write("subprojects/library/build.gradle.kts", "testImplementation(\"example:test:2.0\")")
        write("subprojects/library/README.md", "Usage")
        write("subprojects/library/CHANGELOG.md", "Changelog")
        commit()
        checkVersions()
    }

    @Test
    fun `test and compile only lock changes need no bumps`() {
        releasedRepository()
        write("subprojects/library/gradle.lockfile", """
            example:dependency:1.0=compileClasspath,runtimeClasspath,testRuntimeClasspath
            example:optional:2.0=compileClasspath,testCompileClasspath,testRuntimeClasspath
            example:test:2.0=testCompileClasspath,testRuntimeClasspath
            empty=annotationProcessor
        """.trimIndent())
        commit()
        checkVersions()
    }

    @Test
    fun `removing a runtime dependency requires bumps`() {
        releasedRepository()
        write("subprojects/library/gradle.lockfile", "example:dependency:1.0=compileClasspath\n")
        commit()
        assertRequiredBumps(setOf("library", "middle", "consumer"))
    }

    @Test
    fun `introducing a runtime lockfile requires bumps`() {
        releasedRepository()
        temporaryFolder.resolve("subprojects/library/gradle.lockfile").delete()
        commit()
        git("tag", "-f", "library-1.0.0")
        write("subprojects/library/gradle.lockfile", "example:dependency:1.0=runtimeClasspath\n")
        commit()
        assertRequiredBumps(setOf("library", "middle", "consumer"))
    }
}
