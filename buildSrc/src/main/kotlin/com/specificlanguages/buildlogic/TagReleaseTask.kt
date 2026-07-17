package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * Creates the `<module>-<version>` release tag on `master` after the release commit made by `prepareRelease`
 * has been merged. Tagging is separate from preparation because pull requests are rebase-merged: the merged
 * commit has a different hash than the one created locally, so the tag can only be created once the commit is
 * on `master`. Pushing the tag (which triggers the publish workflow) is left to the caller.
 */
abstract class TagReleaseTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Internal
    abstract val moduleName: Property<String>

    @get:Internal
    abstract val currentVersion: Property<String>

    @get:Internal
    abstract val changelogFile: RegularFileProperty

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun tag() {
        val module = moduleName.get()
        val version = currentVersion.get()
        val root = repositoryRoot.get().asFile
        val tag = "$module-$version"

        if (version.endsWith("-SNAPSHOT")) {
            throw GradleException(
                "'$module' is at snapshot version $version; run prepareRelease and merge its commit first.")
        }
        if (git(execOperations, root, "tag", "--list", tag).output.isNotBlank()) {
            throw GradleException("Tag '$tag' already exists; bump the version in gradle.properties first.")
        }
        val branch = git(execOperations, root, "rev-parse", "--abbrev-ref", "HEAD").output.trim()
        if (branch != "master") {
            throw GradleException(
                "Release tags must be created on 'master' (currently on '$branch') so that they point at the " +
                    "rebase-merged release commit.")
        }
        val changelog = changelogFile.get().asFile
        if (!changelog.readText().contains(Regex("""^## ${Regex.escape(version)}[ \t]*$""", RegexOption.MULTILINE))) {
            throw GradleException(
                "$changelog has no '$version' section; run prepareRelease and merge its commit first.")
        }

        // The message makes the tag annotated, which also satisfies a `tag.gpgsign` git configuration
        // (signing requires a message).
        git(execOperations, root, "tag", "-m", "$module $version", tag)

        logger.lifecycle("Created tag $tag. Publish the release by pushing it: git push origin $tag")
    }
}
