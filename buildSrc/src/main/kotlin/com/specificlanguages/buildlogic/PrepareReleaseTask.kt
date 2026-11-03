package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
 * Before the rename, the changelog's `Dependency updates` subsection is filled in with the changes since the
 * module's last release: new versions of `api`, `implementation` and `runtimeOnly` dependencies (for external
 * ones, the version locked for `runtimeClasspath`), and minor or major updates of the target Gradle version.
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

    /** The module directory, relative to [repositoryRoot]. */
    @get:Internal
    abstract val modulePath: Property<String>

    /** `group:name` of the external `api`, `implementation` and `runtimeOnly` dependencies of the module. */
    @get:Internal
    abstract val directDependencies: SetProperty<String>

    /** Maps the names of the module's project dependencies to their directories, relative to [repositoryRoot]. */
    @get:Internal
    abstract val projectDependencyPaths: MapProperty<String, String>

    @TaskAction
    fun prepare() {
        val module = moduleName.get()
        val version = currentVersion.get().removeSuffix("-SNAPSHOT")
        val git = Git(execOperations, repositoryRoot.get().asFile)
        val tag = "$module-$version"

        if (git.tagExists(tag)) {
            throw GradleException("Tag '$tag' already exists; bump the version in gradle.properties first.")
        }

        val changedFiles = mutableListOf<File>()
        patchChangelog(changelogFile.get().asFile, version, dependencyUpdates(git, module))
            ?.let { changedFiles.add(it) }
        patchVersion(propertiesFile.get().asFile, version)?.let { changedFiles.add(it) }

        if (changedFiles.isEmpty()) {
            logger.lifecycle(
                "The files of '$module' are already in their $version release state; " +
                    "run tagRelease on master to create the release tag.")
            return
        }

        git.commitFiles(changedFiles, "chore($module): release $version")

        logger.lifecycle(
            "Prepared release $version of '$module'. Open a pull request with this commit; " +
                "after it is merged, run tagRelease on master to create the release tag.")
    }

    /** Dependency changes between the module's last release and the working tree; empty for a first release. */
    private fun dependencyUpdates(git: Git, module: String): List<DependencyUpdate> {
        val lastReleased = git.latestReleaseVersion(module) ?: return emptyList()
        val baseline = "$module-$lastReleased"
        val root = repositoryRoot.get().asFile
        fun current(path: String) = root.resolve(path).takeIf { it.isFile }?.readText()
        fun released(path: String) = git.readFileAtRevision(baseline, path)

        val updates = mutableListOf<DependencyUpdate>()

        val wrapper = "gradle/wrapper/gradle-wrapper.properties"
        val releasedGradle = released(wrapper)?.let(::wrapperGradleVersion)
        val currentGradle = current(wrapper)?.let(::wrapperGradleVersion)
        if (releasedGradle != null && currentGradle != null &&
            bumpLevel(releasedGradle, currentGradle) >= ChangeLevel.MINOR
        ) {
            updates.add(
                DependencyUpdate(
                    "Gradle", currentGradle,
                    "The plugin is built with this Gradle version and may not work with older ones."))
        }

        for ((name, path) in projectDependencyPaths.get().toSortedMap()) {
            val properties = "$path/gradle.properties"
            val currentVersion = current(properties)?.let(::propertiesVersion) ?: continue
            if (released(properties)?.let(::propertiesVersion) != currentVersion) {
                updates.add(DependencyUpdate(name, currentVersion))
            }
        }

        val lockfile = "${modulePath.get()}/gradle.lockfile"
        val releasedLocks = released(lockfile)?.let(::runtimeLockedVersions).orEmpty()
        val currentLocks = current(lockfile)?.let(::runtimeLockedVersions).orEmpty()
        for (dependency in directDependencies.get().sorted()) {
            val currentVersion = currentLocks[dependency] ?: continue
            if (releasedLocks[dependency] != currentVersion) {
                updates.add(DependencyUpdate(dependency, currentVersion))
            }
        }

        return updates
    }

    /**
     * Records [updates] in the changelog and renames its `Unreleased` section to [version]; returns the file if it
     * was changed. Dependency updates alone are enough to create the `Unreleased` section.
     */
    private fun patchChangelog(changelog: File, version: String, updates: List<DependencyUpdate>): File? {
        val text = changelog.readText()
        val versionHeading = Regex("""^## ${Regex.escape(version)}[ \t]*$""", RegexOption.MULTILINE)
        val hasUnreleased = UNRELEASED_HEADING.containsMatchIn(text)
        return when {
            !hasUnreleased && versionHeading.containsMatchIn(text) -> null
            !hasUnreleased && updates.isEmpty() -> throw GradleException(
                "$changelog has neither an 'Unreleased' section nor a '$version' section; " +
                    "document the changes before releasing.")
            else -> {
                updates.forEach { logger.lifecycle("Changelog dependency update: ${it.name} ${it.version}") }
                val patched = addDependencyUpdates(text, updates)
                changelog.writeText(patched.replaceFirst(UNRELEASED_HEADING, "## $version"))
                changelog
            }
        }
    }

    /** Sets the `version` property in [properties] to [version]; returns the file if it was changed. */
    private fun patchVersion(properties: File, version: String): File? {
        val text = properties.readText()
        val current = propertiesVersion(text)
            ?: throw GradleException("$properties does not declare a 'version' property.")
        if (current == version) {
            return null
        }
        properties.writeText(text.replaceFirst(VERSION_PROPERTY, "version=$version"))
        return properties
    }
}
