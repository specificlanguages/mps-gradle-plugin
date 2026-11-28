package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DeprecationTest {

    @Test
    fun `applying the plugin emits no deprecation warnings`(@TempDir projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "deprecation-test"""")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("com.specificlanguages.mps")
            }
            """.trimIndent()
        )

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", "--warning-mode=fail")
            .build()
    }
}
