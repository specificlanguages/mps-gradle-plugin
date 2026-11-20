package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Project `:a` depends on project `:b`, which depends on an external library `com.example:ext`. Each project has
 * a main build: `main` in `:a` and `bLibs` in `:b`.
 */
class ProjectDependenciesTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        val repoDir = projectDir.resolve("repo")
        writeZip(repoDir.resolve("com/example/ext/1.0/ext-1.0.zip"), "ext/marker.txt")

        projectDir.resolve("settings.gradle.kts").writeText(
            """
                include("a", "b")
            """.trimIndent()
        )

        val common = """
            repositories {
                maven {
                    url = uri("${repoDir.toURI()}")
                    metadataSources { artifact() }
                }
            }

            // Avoids resolving MPS to compute the dependencies of RunAnt tasks.
            mpsDefaults.mpsHome = layout.projectDirectory.dir("fake-mps")
        """.trimIndent()

        projectDir.resolve("b").mkdirs()
        projectDir.resolve("b/build.gradle.kts").writeText(
            """
                import com.specificlanguages.mps.MainBuild

                plugins {
                    id("com.specificlanguages.mps")
                }

                $common

                dependencies {
                    api("com.example:ext:1.0@zip")
                }

                mpsBuilds {
                    create<MainBuild>("bLibs") {
                        buildSolutionDescriptor = file("b.msd")
                        buildArtifactsDirectory = layout.buildDirectory.dir("artifacts/bLibs")
                    }
                }
            """.trimIndent()
        )

        projectDir.resolve("a").mkdirs()
        projectDir.resolve("a/build.gradle.kts").writeText(
            """
                import com.specificlanguages.mps.MainBuild
                import com.specificlanguages.mps.RunAnt

                plugins {
                    id("com.specificlanguages.mps")
                }

                $common

                dependencies {
                    api(project(":b"))
                }

                mpsBuilds {
                    create<MainBuild>("main") {
                        buildSolutionDescriptor = file("a.msd")
                        buildArtifactsDirectory = layout.buildDirectory.dir("artifacts/main")
                    }
                }

                val generateMainProperties = tasks.named<RunAnt>("generateMain").flatMap { it.pathProperties }
                tasks.register("printArtifactsProperties") {
                    doLast {
                        generateMainProperties.get()
                            .filterKeys { it.startsWith("artifacts.") }
                            .forEach { (key, value) -> println("${'$'}key=${'$'}{value.canonicalPath}") }
                    }
                }

                val generateMainPropertyFiles = tasks.named<RunAnt>("generateMain").map { it.propertyFiles.files }
                tasks.register("printPropertyFiles") {
                    doLast {
                        generateMainPropertyFiles.get().forEach { println("propertyFile=${'$'}{it.canonicalPath}") }
                    }
                }
            """.trimIndent()
        )
    }

    @Test
    fun `setup resolves external libraries of project dependencies without building them`() {
        val result = runner(":a:setup").build()

        assertNotNull(result.task(":b:setup"), ":b:setup should run")
        assertNull(result.task(":b:assembleBLibs"), ":b:assembleBLibs should not run")
        assertNull(result.task(":b:zip"), ":b:zip should not run")

        assertTrue(projectDir.resolve("a/build/dependencies/ext/marker.txt").isFile,
            "external library of :b should be extracted into :a")
        assertThat(projectDir.resolve("a/build/dependencies").list()!!.toList(), contains("ext"))
    }

    @Test
    fun `Ant tasks depend on assembling builds of project dependencies`() {
        val lines = runner(":a:generateMain", "--dry-run").build().output.lines()

        assertThat(lines, containsInRelativeOrder(":b:assembleBLibs SKIPPED", ":a:generateMain SKIPPED"))
        assertThat(lines, not(hasItem(":b:zip SKIPPED")))
    }

    @Test
    fun `Ant tasks receive artifacts locations of builds`() {
        val lines = runner(":a:printArtifactsProperties").build().output.lines()

        val root = projectDir.canonicalFile
        assertThat(lines, hasItems(
            "artifacts.bLibs=${root.resolve("b/build/artifacts/bLibs").path}",
            "artifacts.main=${root.resolve("a/build/artifacts/main").path}",
        ))
    }

    @Test
    fun `setup writes artifacts properties for top-level directories of external libraries`() {
        runner(":a:setup").build()

        val properties = Properties()
        projectDir.resolve("a/build/mps-libraries-artifacts.properties").inputStream().use { properties.load(it) }

        assertThat(properties.keys, contains("artifacts.ext"))
        assertEquals(
            projectDir.resolve("a/build/dependencies/ext").canonicalPath,
            File(properties.getProperty("artifacts.ext")).canonicalPath
        )
    }

    @Test
    fun `Ant tasks load artifacts properties of external libraries`() {
        val dryRunLines = runner(":a:generateMain", "--dry-run").build().output.lines()
        assertThat(dryRunLines, containsInRelativeOrder(":a:resolveMpsLibraries SKIPPED", ":a:generateMain SKIPPED"))

        val lines = runner(":a:printPropertyFiles").build().output.lines()
        val expected = projectDir.canonicalFile.resolve("a/build/mps-libraries-artifacts.properties").path
        assertThat(lines, hasItem("propertyFile=$expected"))
    }

    private fun runner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*arguments)

    private fun writeZip(file: File, entryName: String) {
        file.parentFile.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(entryName))
            zip.write("marker".toByteArray())
            zip.closeEntry()
        }
    }
}
