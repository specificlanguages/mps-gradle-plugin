package com.specificlanguages.mps

import org.gradle.testkit.runner.GradleRunner
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class BuildScriptDependenciesTest {

    @Test
    fun `dependsOn translates to task dependency`(@TempDir tempDir: Path) {
        tempDir.resolve("build.gradle.kts").writeText(
            """
                import com.specificlanguages.mps.MainBuild
                import com.specificlanguages.mps.TestBuild

                plugins {
                    id("com.specificlanguages.mps")
                }

                dependencies {
                    mps("com.jetbrains:mps:2024.3.1")
                }
                
                repositories {
                    maven("https://artifacts.itemis.cloud/repository/maven-mps")
                    mavenCentral()
                }
                
                mpsBuilds {
                    val main = create<MainBuild>("main") {
                        buildSolutionDescriptor = file("foo.msd")
                    }

                    create<TestBuild>("test") {
                        dependsOn(main)
                        buildSolutionDescriptor = file("bar.msd")
                    }
                }
            """.trimIndent()
        )

        val buildResult = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("checkTest", "--dry-run")
            .build()

        MatcherAssert.assertThat(
            buildResult.output.lines(),
            Matchers.containsInRelativeOrder(":assembleMain SKIPPED", ":checkTest SKIPPED")
        )
    }

}