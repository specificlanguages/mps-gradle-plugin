package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/** A module and the internal modules it depends on, captured at configuration time. */
data class ModuleInfo(
    val name: String,
    val version: String,
    val path: String,
    val dependencies: Set<String>
) : java.io.Serializable

/**
 * Fails if a released module changed since its last release without every module that (transitively) depends
 * on it being bumped as well. Dependents must carry a fix in the changed dependency in a new release of their
 * own, so each is required to have at least a patch bump over its last released version.
 */
abstract class CheckReleaseVersionsTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    abstract val modules: ListProperty<ModuleInfo>

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val root = repositoryRoot.get().asFile
        val modules = modules.get()
        val byName = modules.associateBy { it.name }

        val lastReleased = modules.associate { it.name to latestReleaseVersion(execOperations, root, it.name) }
        val changed = modules.filter { hasChangedSinceRelease(root, it, lastReleased.getValue(it.name)) }.map { it.name }

        val violations = mutableListOf<String>()
        for (changedModule in changed) {
            for (dependent in transitiveDependents(changedModule, byName)) {
                val info = byName.getValue(dependent)
                val baseline = lastReleased.getValue(dependent) ?: continue
                val target = info.version.removeSuffix("-SNAPSHOT")
                if (bumpLevel(baseline, target) == ChangeLevel.NONE) {
                    violations.add(
                        "'$changedModule' changed since its release; dependent '$dependent' must be bumped at " +
                            "least a patch over $baseline (currently $target).")
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Release version check failed:\n" + violations.distinct().joinToString("\n") { "  - $it" })
        }
    }

    private fun hasChangedSinceRelease(root: java.io.File, module: ModuleInfo, lastReleased: String?): Boolean {
        // A module that has never been released is treated as changed: it must be released to carry any fix.
        if (lastReleased == null) return true
        val diff = git(execOperations, root, "diff", "--quiet", "${module.name}-$lastReleased", "HEAD", "--",
            module.path, ignoreExitValue = true)
        return diff.exitCode != 0
    }

    private fun transitiveDependents(module: String, byName: Map<String, ModuleInfo>): Set<String> {
        val dependents = mutableSetOf<String>()
        val queue = ArrayDeque(byName.values.filter { module in it.dependencies }.map { it.name })
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (dependents.add(next)) {
                byName.values.filter { next in it.dependencies }.forEach { queue.add(it.name) }
            }
        }
        return dependents
    }
}
