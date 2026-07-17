package com.specificlanguages.buildlogic

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal data class GitResult(val exitCode: Int, val output: String)

internal fun git(
    exec: ExecOperations,
    repositoryRoot: File,
    vararg args: String,
    ignoreExitValue: Boolean = false
): GitResult {
    val output = ByteArrayOutputStream()
    val result = exec.exec {
        workingDir = repositoryRoot
        commandLine = listOf("git") + args
        standardOutput = output
        isIgnoreExitValue = ignoreExitValue
    }
    return GitResult(result.exitValue, output.toString(Charsets.UTF_8))
}

/** The highest release version among tags named `<module>-<version>`, or `null` if the module has none. */
internal fun latestReleaseVersion(exec: ExecOperations, repositoryRoot: File, module: String): String? {
    val tags = git(exec, repositoryRoot, "tag", "--list", "$module-*").output.lines().filter { it.isNotBlank() }
    return selectLatestRelease(tags.map { it.removePrefix("$module-") })
}
