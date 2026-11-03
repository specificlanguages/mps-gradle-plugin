package com.specificlanguages.buildlogic

/**
 * A dependency whose version changed since the last release, listed in the changelog as `- <name> <version>`,
 * followed by [note] when the line is newly added.
 */
data class DependencyUpdate(val name: String, val version: String, val note: String? = null) {
    fun toBullet(): String = "- $name $version" + (note?.let { ". $it" } ?: "")
}

/** The `group:name:version` coordinates of a lockfile's `runtimeClasspath` entries. */
internal fun runtimeLockEntries(lockfile: String): Set<String> =
    lockfile.lineSequence()
        .filter { !it.startsWith("#") && '=' in it }
        .filter { "runtimeClasspath" in it.substringAfter('=').split(',') }
        .map { it.substringBefore('=') }
        .filter { it != "empty" }
        .toSet()

/** Maps `group:name` to the version locked for `runtimeClasspath`. */
internal fun runtimeLockedVersions(lockfile: String): Map<String, String> =
    runtimeLockEntries(lockfile).associate { it.substringBeforeLast(':') to it.substringAfterLast(':') }

internal val VERSION_PROPERTY = Regex("""^version=(.*)$""", RegexOption.MULTILINE)

/** The value of the `version` property in a `gradle.properties` file, or `null` if it is not declared. */
internal fun propertiesVersion(properties: String): String? =
    VERSION_PROPERTY.find(properties)?.groupValues?.get(1)?.trim()

/** The Gradle version of a `gradle-wrapper.properties` file's distribution URL, or `null` if it has none. */
internal fun wrapperGradleVersion(properties: String): String? =
    Regex("""^distributionUrl=.*/gradle-(.+)-(?:bin|all)\.zip\s*$""", RegexOption.MULTILINE)
        .find(properties)?.groupValues?.get(1)

internal val UNRELEASED_HEADING = Regex("""^## Unreleased[ \t]*$""", RegexOption.MULTILINE)
private val DEPENDENCY_UPDATES_HEADING = Regex("""^### Dependency updates[ \t]*$""")

/**
 * Records [updates] in the `### Dependency updates` subsection of the changelog's `Unreleased` section, creating
 * the section and the subsection when missing. A bullet already naming a dependency gets the new version and keeps
 * the rest of its text; other updates are appended as new bullets.
 */
internal fun addDependencyUpdates(changelog: String, updates: List<DependencyUpdate>): String {
    if (updates.isEmpty()) return changelog
    val lines = changelog.lines().toMutableList()

    var unreleased = lines.indexOfFirst { UNRELEASED_HEADING.matches(it) }
    if (unreleased < 0) {
        val firstRelease = lines.indexOfFirst { it.startsWith("## ") }
        unreleased = if (firstRelease < 0) lines.size else firstRelease
        lines.addAll(unreleased, listOf("## Unreleased", ""))
    }

    fun sectionEnd() = (unreleased + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size

    var subsection = (unreleased + 1 until sectionEnd()).firstOrNull { DEPENDENCY_UPDATES_HEADING.matches(lines[it]) }
    if (subsection == null) {
        val end = sectionEnd()
        var insertAt = end
        while (insertAt - 1 > unreleased && lines[insertAt - 1].isBlank()) insertAt--
        val block = mutableListOf("", "### Dependency updates", "")
        lines.addAll(insertAt, block)
        subsection = insertAt + 1
        // Keep a blank line between the new subsection and the next release section.
        if (insertAt + block.size < lines.size && lines[insertAt + block.size].isNotBlank()) {
            lines.add(insertAt + block.size, "")
        }
    }

    for (update in updates) {
        val subsectionEnd = (subsection + 1 until lines.size).firstOrNull { lines[it].startsWith("#") } ?: lines.size
        // The version ends before whitespace, `,`, `;`, or a `.` that ends a sentence.
        val bullet = Regex("""^- ${Regex.escape(update.name)} \d(?:[^\s,;]*[^\s,;.])?(.*)$""")
        val existing = (subsection + 1 until subsectionEnd).firstOrNull { bullet.matches(lines[it]) }
        if (existing != null) {
            val rest = bullet.matchEntire(lines[existing])!!.groupValues[1]
            lines[existing] = "- ${update.name} ${update.version}$rest"
        } else {
            val lastContent = (subsection + 1 until subsectionEnd).lastOrNull { lines[it].isNotBlank() }
            if (lastContent != null) {
                lines.add(lastContent + 1, update.toBullet())
            } else {
                if (subsection + 1 >= lines.size || lines[subsection + 1].isNotBlank()) lines.add(subsection + 1, "")
                lines.add(subsection + 2, update.toBullet())
            }
        }
    }
    return lines.joinToString("\n")
}
