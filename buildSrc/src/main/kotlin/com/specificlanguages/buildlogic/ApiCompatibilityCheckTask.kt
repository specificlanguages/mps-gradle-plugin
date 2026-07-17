package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Fails if the version bump declared for this module is smaller than the change to its public API requires.
 *
 * The baseline is the module's last release tag (`<module>-<version>`); its public API is the
 * binary-compatibility-validator `.api` dump committed at that tag. Comparing it against the current `.api`
 * dump yields the required change level, which is checked against the actual `baseline -> current` bump.
 */
abstract class ApiCompatibilityCheckTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val targetVersion: Property<String>

    @get:InputFiles
    abstract val currentApiFile: ConfigurableFileCollection

    /** Path of the `.api` dump relative to the repository root, used to read the baseline from a git tag. */
    @get:Input
    abstract val apiFilePath: Property<String>

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val module = moduleName.get()

        val tags = git("tag", "--list", "$module-*").lines().filter { it.isNotBlank() }
        val baselineVersion = selectLatestRelease(tags.map { it.removePrefix("$module-") })
        if (baselineVersion == null) {
            logger.lifecycle("No released baseline for '$module'; skipping API compatibility check.")
            return
        }

        val baselineApi = git("show", "$module-$baselineVersion:${apiFilePath.get()}", ignoreExitValue = true)
        val currentApi = currentApiFile.files.singleOrNull()?.takeIf { it.exists() }?.readText().orEmpty()

        val required = requiredLevelFromApiDiff(baselineApi, currentApi)
        val actual = bumpLevel(baselineVersion, targetVersion.get())
        if (actual.ordinal < required.ordinal) {
            val actualText = if (actual == ChangeLevel.NONE) "no bump" else "only a ${actual.name.lowercase()} bump"
            throw GradleException(
                "API compatibility check for '$module': changes since $baselineVersion require a " +
                    "${required.name.lowercase()} version bump, but $baselineVersion -> ${targetVersion.get()} " +
                    "is $actualText.")
        }
    }

    private fun git(vararg args: String, ignoreExitValue: Boolean = false): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            workingDir = repositoryRoot.get().asFile
            commandLine = listOf("git") + args
            standardOutput = output
            isIgnoreExitValue = ignoreExitValue
        }
        return output.toString(Charsets.UTF_8)
    }
}
