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
    fun canRunJava(@TempDir testProjectDir: File) {
        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.jbr-toolchain")
            }
            
            dependencies {
                jbr("com.jetbrains.jdk:jbr_jcef:$JBR_VERSION")
            }
            
            repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps")

            val javaVersion by tasks.registering(JavaExec::class) {
                javaLauncher = jbrToolchain.javaLauncher
                jvmArgs("-version")
                mainClass = "dummy"
            }
            """.trimIndent()
        )

        val task = ":javaVersion"
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(task)
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(task)?.outcome)
        assertTrue(result.output.contains(JBR_BUILD)) {
            "output should contain JBR version $JBR_BUILD but was: ${result.output}"
        }
    }

    @Test
    fun projectsShareJbr(@TempDir project1Dir: File, @TempDir project2Dir: File) {
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
    
                val printJavaHome by tasks.registering {
                    doLast {
                        val javaHome = jbrToolchain.javaLauncher.get().metadata.installationPath
                        println("Java home: ${'$'}javaHome")
                
                    }
                }
                """.trimIndent()
            )
        }

        val javaHomes = projectDirs.map { projectDir ->
            val result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(":printJavaHome")
                .withPluginClasspath()
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":printJavaHome")?.outcome, "$projectDir outcome of :printJavaHome")

            val javaHomeMatch = Regex("^Java home:(.*)$", RegexOption.MULTILINE).find(result.output)
            assertTrue(javaHomeMatch != null, "$projectDir output should contain 'Java home:' but was: ${result.output}")

            javaHomeMatch!!.groupValues[1]
        }

        assertEquals(javaHomes[0], javaHomes[1], "Java home should be the same in both projects")
    }

    @Test
    fun errorMessageWhenEmpty() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(JbrToolchainPlugin::class.java)

        val jbrToolchain = project.extensions.getByType(JbrToolchainExtension::class.java)

        val exception = assertThrows(IllegalStateException::class.java) { jbrToolchain.javaLauncher.get() }

        assertThat(
            exception.message,
            containsString("Expected a single dependency for configuration 'jbr', found 0 dependencies")
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
            containsString("Expected a single dependency for configuration 'jbr', found 2 dependencies")
        )
    }

}
