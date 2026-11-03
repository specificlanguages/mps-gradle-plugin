package com.specificlanguages.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DependencyUpdatesTest {
    private val header = """
        # Changelog

        Intro.

    """.trimIndent()

    private val previousRelease = """
        ## 1.0.0

        ### Added

        - Something.
    """.trimIndent()

    @Test
    fun `no updates leave the changelog unchanged`() {
        val changelog = "$header\n$previousRelease\n"
        assertEquals(changelog, addDependencyUpdates(changelog, emptyList()))
    }

    @Test
    fun `creates the subsection at the end of the Unreleased section`() {
        val changelog = "$header\n## Unreleased\n\n### Fixed\n\n- A bug.\n\n$previousRelease\n"
        val expected = "$header\n## Unreleased\n\n### Fixed\n\n- A bug.\n\n" +
            "### Dependency updates\n\n- lib 2.0\n\n$previousRelease\n"
        assertEquals(expected, addDependencyUpdates(changelog, listOf(DependencyUpdate("lib", "2.0"))))
    }

    @Test
    fun `creates the Unreleased section when missing`() {
        val changelog = "$header\n$previousRelease\n"
        val expected = "$header\n## Unreleased\n\n### Dependency updates\n\n- lib 2.0\n\n$previousRelease\n"
        assertEquals(expected, addDependencyUpdates(changelog, listOf(DependencyUpdate("lib", "2.0"))))
    }

    @Test
    fun `updates the version of an existing bullet and keeps its text`() {
        val changelog = "$header\n## Unreleased\n\n### Dependency updates\n\n" +
            "- lib 1.1.0-SNAPSHOT, enabling something.\n- other 3.0\n\n$previousRelease\n"
        val expected = "$header\n## Unreleased\n\n### Dependency updates\n\n" +
            "- lib 1.1.0, enabling something.\n- other 3.0\n- new 1.0\n\n$previousRelease\n"
        val updates = listOf(DependencyUpdate("lib", "1.1.0"), DependencyUpdate("new", "1.0"))
        assertEquals(expected, addDependencyUpdates(changelog, updates))
    }

    @Test
    fun `keeps a period that follows the version`() {
        val changelog = "$header\n## Unreleased\n\n### Dependency updates\n\n- Gradle 9.7.0. Some note.\n- lib 1.0.\n"
        val expected = "$header\n## Unreleased\n\n### Dependency updates\n\n- Gradle 9.8.2. Some note.\n- lib 2.0.\n"
        val updates = listOf(DependencyUpdate("Gradle", "9.8.2"), DependencyUpdate("lib", "2.0"))
        assertEquals(expected, addDependencyUpdates(changelog, updates))
    }

    @Test
    fun `does not touch bullets of earlier releases`() {
        val release = "## 1.0.0\n\n### Dependency updates\n\n- lib 1.0\n"
        val changelog = "$header\n## Unreleased\n\n### Added\n\n- Feature.\n\n$release"
        val expected =
            "$header\n## Unreleased\n\n### Added\n\n- Feature.\n\n### Dependency updates\n\n- lib 2.0\n\n$release"
        assertEquals(expected, addDependencyUpdates(changelog, listOf(DependencyUpdate("lib", "2.0"))))
    }

    @Test
    fun `adds the note of a new bullet`() {
        val changelog = "$header\n## Unreleased\n"
        val expected = "$header\n## Unreleased\n\n### Dependency updates\n\n- Gradle 9.8.2. Note.\n"
        assertEquals(expected, addDependencyUpdates(changelog, listOf(DependencyUpdate("Gradle", "9.8.2", "Note."))))
    }

    @Test
    fun `reads versions from lockfiles, properties, and the wrapper`() {
        val lockfile = "# comment\na:b:1.0=compileClasspath,runtimeClasspath\nc:d:2.0=testRuntimeClasspath\n" +
            "empty=annotationProcessor\n"
        assertEquals(mapOf("a:b" to "1.0"), runtimeLockedVersions(lockfile))
        assertEquals("1.2.0-SNAPSHOT", propertiesVersion("group=x\nversion=1.2.0-SNAPSHOT\n"))
        assertEquals(
            "9.8.2",
            wrapperGradleVersion("distributionUrl=https\\://services.gradle.org/distributions/gradle-9.8.2-bin.zip\n"))
    }
}
