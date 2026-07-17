package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Prepares the release of one module: drops the `-SNAPSHOT` suffix from the version in the module's
 * `gradle.properties`, renames the changelog's `Unreleased` section to the release version and commits both
 * files. The commit is meant to reach `master` through a pull request; because pull requests are
 * rebase-merged (giving the commit a new hash), the release tag is created separately by the `tagRelease`
 * task once the commit is on `master`.
 *
 * Files already in their release state are left alone, so a run that was aborted halfway can be repeated.
 */
abstract class PrepareReleaseTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Internal
    abstract val moduleName: Property<String>

    @get:Internal
    abstract val currentVersion: Property<String>

    @get:Internal
    abstract val changelogFile: RegularFileProperty

    @get:Internal
    abstract val propertiesFile: RegularFileProperty

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun prepare() {
        val module = moduleName.get()
        val version = currentVersion.get().removeSuffix("-SNAPSHOT")
        val root = repositoryRoot.get().asFile
        val tag = "$module-$version"

        if (git(execOperations, root, "tag", "--list", tag).output.isNotBlank()) {
            throw GradleException("Tag '$tag' already exists; bump the version in gradle.properties first.")
        }

        val changedFiles = mutableListOf<File>()
        patchChangelog(changelogFile.get().asFile, version)?.let { changedFiles.add(it) }
        patchVersion(propertiesFile.get().asFile, version)?.let { changedFiles.add(it) }

        if (changedFiles.isEmpty()) {
            logger.lifecycle(
                "The files of '$module' are already in their $version release state; " +
                    "run tagRelease on master to create the release tag.")
            return
        }

        val paths = changedFiles
            .map { root.toPath().relativize(it.toPath()).toString().replace('\\', '/') }
            .toTypedArray()
        git(execOperations, root, "commit", "-m", "chore($module): release $version", "--", *paths)

        logger.lifecycle(
            "Prepared release $version of '$module'. Open a pull request with this commit; " +
                "after it is merged, run tagRelease on master to create the release tag.")
    }

    /** Renames the changelog's `Unreleased` section to [version]; returns the file if it was changed. */
    private fun patchChangelog(changelog: File, version: String): File? {
        val text = changelog.readText()
        val unreleasedHeading = Regex("""^## Unreleased[ \t]*$""", RegexOption.MULTILINE)
        return when {
            unreleasedHeading.containsMatchIn(text) -> {
                changelog.writeText(text.replaceFirst(unreleasedHeading, "## $version"))
                changelog
            }
            text.contains(Regex("""^## ${Regex.escape(version)}[ \t]*$""", RegexOption.MULTILINE)) -> null
            else -> throw GradleException(
                "$changelog has neither an 'Unreleased' section nor a '$version' section; " +
                    "document the changes before releasing.")
        }
    }

    /** Sets the `version` property in [properties] to [version]; returns the file if it was changed. */
    private fun patchVersion(properties: File, version: String): File? {
        val text = properties.readText()
        val versionLine = Regex("""^version=(.*)$""", RegexOption.MULTILINE)
        val match = versionLine.find(text)
            ?: throw GradleException("$properties does not declare a 'version' property.")
        if (match.groupValues[1] == version) {
            return null
        }
        properties.writeText(text.replaceFirst(versionLine, "version=$version"))
        return properties
    }
}
