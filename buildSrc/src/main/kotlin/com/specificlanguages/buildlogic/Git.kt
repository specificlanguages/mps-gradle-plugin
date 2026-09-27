package com.specificlanguages.buildlogic

import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal class Git(
    private val exec: ExecOperations,
    private val repositoryRoot: File
) {
    private fun git(vararg args: String): String {
        val output = ByteArrayOutputStream()
        exec.exec {
            workingDir = repositoryRoot
            commandLine = listOf("git") + args
            standardOutput = output
        }
        return output.toString(Charsets.UTF_8)
    }

    /** The highest release version among tags named `<module>-<version>`, or `null` if the module has none. */
    fun latestReleaseVersion(module: String): String? {
        val tags = git("tag", "--list", "$module-*").lines().filter { it.isNotBlank() }
        return selectLatestRelease(tags.map { it.removePrefix("$module-") })
    }

    fun tagExists(tag: String): Boolean =
        git("tag", "--list", tag).isNotBlank()

    fun currentBranch(): String =
        git("rev-parse", "--abbrev-ref", "HEAD").trim()

    /** Returns the file contents at [revision], or `null` if the path does not exist there. */
    fun readFileAtRevision(revision: String, path: String): String? {
        if (git("ls-tree", "--name-only", revision, "--", path).isBlank()) {
            return null
        }
        return git("show", "$revision:$path")
    }

    /** Returns changed paths relative to the repository root. */
    fun changedPaths(
        fromRevision: String,
        toRevision: String,
        path: String
    ): List<String> =
        git("diff", "--name-only", fromRevision, toRevision, "--", path)
            .lines().filter { it.isNotBlank() }

    fun commitFiles(files: List<File>, message: String) {
        val paths = files.map { repositoryRoot.toPath().relativize(it.toPath()).toString().replace('\\', '/') }
            .toTypedArray()
        git("commit", "-m", message, "--", *paths)
    }

    fun createAnnotatedTag(tag: String, message: String) {
        // The message also satisfies a `tag.gpgsign` git configuration (signing requires a message).
        git("tag", "-m", message, tag)
    }
}
