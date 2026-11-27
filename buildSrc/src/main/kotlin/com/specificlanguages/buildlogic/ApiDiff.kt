package com.specificlanguages.buildlogic

/**
 * Derives the minimum required [ChangeLevel] from the difference between two binary-compatibility-validator
 * `.api` dumps: any removed or changed declaration requires [ChangeLevel.MAJOR], a purely added declaration
 * requires [ChangeLevel.MINOR], and no difference requires [ChangeLevel.NONE].
 *
 * A signature change appears as one removed declaration plus one added declaration, so it is (correctly)
 * treated as a breaking change.
 *
 * Constructors of Gradle plugin classes (classes implementing `org.gradle.api.Plugin`) are ignored: Gradle
 * instantiates plugins itself and supplies the constructor parameters by injection, so adding or removing an
 * injected service does not affect consumers of the plugin.
 */
fun requiredLevelFromApiDiff(baselineApi: String, currentApi: String): ChangeLevel {
    val baseline = declarations(baselineApi)
    val current = declarations(currentApi)
    return when {
        (baseline - current).isNotEmpty() -> ChangeLevel.MAJOR
        (current - baseline).isNotEmpty() -> ChangeLevel.MINOR
        else -> ChangeLevel.NONE
    }
}

/**
 * The set of API declarations in a `.api` dump. Each member declaration is qualified by its enclosing class
 * so that identical member signatures in different classes remain distinct, and so that removing a class also
 * removes its members from the set.
 */
private fun declarations(api: String): Set<String> {
    val result = mutableSetOf<String>()
    var enclosingClass = ""
    var enclosingClassIsPlugin = false
    for (line in api.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed == "}") continue
        if (line.startsWith("\t") || line.startsWith(" ")) {
            if (enclosingClassIsPlugin && isConstructor(trimmed)) continue
            result.add("$enclosingClass => $trimmed")
        } else {
            enclosingClass = trimmed
            enclosingClassIsPlugin = isPluginClass(trimmed)
            result.add("class $trimmed")
        }
    }
    return result
}

/** Whether a class header line of a `.api` dump declares a class that implements `org.gradle.api.Plugin`. */
private fun isPluginClass(header: String): Boolean =
    header.substringAfter(" : ", "").removeSuffix("{").split(",").any { it.trim() == "org/gradle/api/Plugin" }

/** Whether a member line of a `.api` dump declares a constructor. */
private fun isConstructor(member: String): Boolean = " <init> (" in member
