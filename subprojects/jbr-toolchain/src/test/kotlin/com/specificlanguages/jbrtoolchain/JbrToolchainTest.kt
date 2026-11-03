package com.specificlanguages.jbrtoolchain

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class JbrToolchainTest {
    companion object {
        private const val JBR_BUILD = "17.0.6+7-469.82"
        private const val JBR_VERSION = "17.0.6-b469.82"
    }

    @Test
    fun `java launcher stores and reuses configuration cache`(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }
            
            dependencies {
                jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
            }
            
            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            mpsPlatformCache.cacheRoot.set(layout.projectDirectory.dir("cache"))

            val javaVersion by tasks.registering(JavaExec::class) {
                javaLauncher = jbrToolchain.javaLauncher
                jvmArgs("-version")
                mainClass = "dummy"
            }
            """.trimIndent()
        )

        val task = ":javaVersion"
        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(task, "--configuration-cache", "--configuration-cache-problems=fail")
            .withPluginClasspath()

        val result = runner.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
        assertTrue(result.output.contains(JBR_BUILD)) {
            "output should contain JBR version $JBR_BUILD but was: ${result.output}"
        }
        assertTrue(result.output.contains("Configuration cache entry stored."), result.output)

        val reused = runner.build()
        assertEquals(TaskOutcome.SUCCESS, reused.task(task)?.outcome)
        assertTrue(reused.output.contains("Reusing configuration cache."), reused.output)
        assertTrue(reused.output.contains(JBR_BUILD), reused.output)
    }

    @Test
    fun `selectPlatform resolves JBR for given platforms directly and through a marker`(
        @TempDir testProjectDir: File,
        @TempDir repoDir: File
    ) {
        val jbrVersion = "1.0-test"
        val markerVersion = "1.0-test"

        val jbrDir = repoDir.resolve("com/jetbrains/jdk/jbr_jcef/$jbrVersion").apply { mkdirs() }
        jbrDir.resolve("jbr_jcef-$jbrVersion.pom").writeText(pom("com.jetbrains.jdk", "jbr_jcef", jbrVersion, ""))
        for (classifier in listOf("windows-x64", "linux-aarch64", "osx-aarch64")) {
            jbrDir.resolve("jbr_jcef-$jbrVersion-$classifier.tgz").writeText(classifier)
        }

        repoDir.resolve("com/jetbrains/mps/mps-jbr/$markerVersion").apply { mkdirs() }
            .resolve("mps-jbr-$markerVersion.pom")
            .writeText(
                pom(
                    "com.jetbrains.mps", "mps-jbr", markerVersion,
                    """
                    <dependencies>
                      <dependency>
                        <groupId>com.jetbrains.jdk</groupId>
                        <artifactId>jbr_jcef</artifactId>
                        <version>$jbrVersion</version>
                      </dependency>
                    </dependencies>
                    """
                )
            )

        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            import com.specificlanguages.jbrtoolchain.JbrPlatform

            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }

            repositories.maven("${repoDir.toURI()}")

            val jbrWin = configurations.register("jbrWin") {
                isCanBeConsumed = false
                jbrToolchain.selectPlatform(this, JbrPlatform.WINDOWS_X64)
            }
            val jbrLinuxAarch64 = configurations.register("jbrLinuxAarch64") {
                isCanBeConsumed = false
                jbrToolchain.selectPlatform(this, JbrPlatform.LINUX_AARCH64)
            }
            val jbrMacAarch64 = configurations.register("jbrMacAarch64") {
                isCanBeConsumed = false
                jbrToolchain.selectPlatform(this, JbrPlatform.OSX_AARCH64)
            }

            dependencies {
                jbrWin("com.jetbrains.jdk:jbr_jcef:$jbrVersion")
                jbrLinuxAarch64("com.jetbrains.mps:mps-jbr:$markerVersion")
                jbrMacAarch64("com.jetbrains.mps:mps-jbr:$markerVersion")
            }

            val printFiles by tasks.registering {
                val files = listOf(jbrWin, jbrLinuxAarch64, jbrMacAarch64).map { config ->
                    config.map { c -> c.name to c.incoming.files.elements.map { it.map { f -> f.asFile.name } } }
                }
                doLast {
                    files.forEach {
                        val (name, fileNames) = it.get()
                        println("${'$'}name: ${'$'}{fileNames.get()}")
                    }
                }
            }
            """.trimIndent()
        )

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(":printFiles")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printFiles")?.outcome)
        assertThat(result.output, containsString("jbrWin: [jbr_jcef-$jbrVersion-windows-x64.tgz]"))
        assertThat(result.output, containsString("jbrLinuxAarch64: [jbr_jcef-$jbrVersion-linux-aarch64.tgz]"))
        assertThat(result.output, containsString("jbrMacAarch64: [jbr_jcef-$jbrVersion-osx-aarch64.tgz]"))
    }

    @Test
    fun errorMessageWhenEmpty() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JbrToolchainPlugin::class.java)

        val jbrToolchain = project.extensions.getByType(JbrToolchainExtension::class.java)

        val exception = assertThrows(IllegalStateException::class.java) { jbrToolchain.javaLauncher.get() }

        assertThat(
            exception.message,
            containsString("Expected configuration 'jbr' to resolve to a single artifact, but it resolved to 0 artifacts")
        )
    }

    @Test
    fun errorMessageWhenSeveral() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JbrToolchainPlugin::class.java)
        project.dependencies.add("jbr", "com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
        project.dependencies.add("jbr", "com.jetbrains.jdk:jbr:$JBR_VERSION")

        project.repositories.maven {
            url = project.uri("https://artifacts.itemis.cloud/repository/maven-mps")
        }

        val jbrToolchain = project.extensions.getByType(JbrToolchainExtension::class.java)

        val exception = assertThrows(IllegalStateException::class.java) { jbrToolchain.javaLauncher.get() }

        assertThat(
            exception.message,
            containsString("Expected configuration 'jbr' to resolve to a single artifact, but it resolved to 2 artifacts")
        )
    }

    private fun pom(groupId: String, artifactId: String, version: String, body: String) =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          <groupId>$groupId</groupId>
          <artifactId>$artifactId</artifactId>
          <version>$version</version>
          <packaging>pom</packaging>
          $body
        </project>
        """.trimIndent()
}
