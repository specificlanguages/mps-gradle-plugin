package com.specificlanguages.buildlogic

/** A semantic-version change level, ordered from smallest to largest. */
enum class ChangeLevel { NONE, PATCH, MINOR, MAJOR }

internal data class Version(val major: Int, val minor: Int, val patch: Int)

private val RELEASE_VERSION = Regex("""\d+\.\d+\.\d+""")

/**
 * Parses the numeric `major.minor.patch` core of a version, ignoring any pre-release or build suffix
 * (everything from the first `-` or `+`) and any components beyond the third.
 */
internal fun parseVersion(version: String): Version {
    val core = version.substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    fun part(index: Int) = parts.getOrNull(index)?.toIntOrNull() ?: 0
    return Version(part(0), part(1), part(2))
}

private val VERSION_ORDER = Comparator<String> { a, b ->
    compareValuesBy(parseVersion(a), parseVersion(b), { it.major }, { it.minor }, { it.patch })
}

/**
 * The change level represented by moving from [baseline] to [target]. A target that is equal to or lower
 * than the baseline yields [ChangeLevel.NONE].
 */
fun bumpLevel(baseline: String, target: String): ChangeLevel {
    val from = parseVersion(baseline)
    val to = parseVersion(target)
    return when {
        to.major > from.major -> ChangeLevel.MAJOR
        to.major < from.major -> ChangeLevel.NONE
        to.minor > from.minor -> ChangeLevel.MINOR
        to.minor < from.minor -> ChangeLevel.NONE
        to.patch > from.patch -> ChangeLevel.PATCH
        else -> ChangeLevel.NONE
    }
}

/**
 * The highest plain release version (`major.minor.patch` with no pre-release suffix) among [versions],
 * or `null` if there is none.
 */
fun selectLatestRelease(versions: List<String>): String? =
    versions.filter { RELEASE_VERSION.matches(it) }.maxWithOrNull(VERSION_ORDER)
