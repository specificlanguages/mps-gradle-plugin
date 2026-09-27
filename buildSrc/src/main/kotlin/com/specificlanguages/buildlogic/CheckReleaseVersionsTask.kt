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
 * Requires at least a patch bump for a changed module and all its transitive dependents.
 * Tests, Markdown documentation, and build scripts are excluded; dependencies are compared through runtime locks.
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
        val git = Git(execOperations, repositoryRoot.get().asFile)
        val modules = modules.get()
        val byName = modules.associateBy { it.name }

        val lastReleased = modules.associate { it.name to git.latestReleaseVersion(it.name) }
        val changed = modules.filter { hasChangedSinceRelease(git, it, lastReleased.getValue(it.name)) }.map { it.name }

        val violations = mutableListOf<String>()
        for (changedModule in changed) {
            for (dependent in dependentsClosure(changedModule, byName)) {
                val info = byName.getValue(dependent)
                val baseline = lastReleased.getValue(dependent) ?: continue
                val target = info.version.removeSuffix("-SNAPSHOT")
                if (bumpLevel(baseline, target) == ChangeLevel.NONE) {
                    val role = if (dependent == changedModule) "module" else "dependent"
                    violations.add(
                        "'$changedModule' changed since its release; $role '$dependent' must be bumped at " +
                            "least a patch over $baseline (currently $target).")
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Release version check failed:\n" + violations.distinct().joinToString("\n") { "  - $it" })
        }
    }

    private fun hasChangedSinceRelease(git: Git, module: ModuleInfo, lastReleased: String?): Boolean {
        // A module that has never been released is treated as changed: it must be released to carry any fix.
        if (lastReleased == null) return true
        val baseline = "${module.name}-$lastReleased"
        val changedPaths = git.changedPaths(baseline, "HEAD", module.path)
            .map { it.removePrefix("${module.path}/") }
        if (changedPaths.any { !isExcludedPath(it) }) return true
        return "gradle.lockfile" in changedPaths &&
            runtimeDependencies(git, baseline, module) != runtimeDependencies(git, "HEAD", module)
    }

    private fun isExcludedPath(path: String): Boolean =
        path == "gradle.lockfile" || path == "build.gradle.kts" || path == "build.gradle" || path.endsWith(".md") ||
            path.startsWith("src/test/") || path.startsWith("etc/test-projects/")

    private fun runtimeDependencies(git: Git, revision: String, module: ModuleInfo): Set<String> {
        val path = "${module.path}/gradle.lockfile"
        val lockfile = git.readFileAtRevision(revision, path) ?: return emptySet()
        return lockfile.lineSequence()
            .filter { !it.startsWith("#") && '=' in it }
            .filter { "runtimeClasspath" in it.substringAfter('=').split(',') }
            .map { it.substringBefore('=') }
            .filter { it != "empty" }
            .toSet()
    }

    private fun dependentsClosure(module: String, byName: Map<String, ModuleInfo>): Set<String> {
        val dependents = mutableSetOf<String>()
        val queue = ArrayDeque(listOf(module))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (dependents.add(next)) {
                byName.values.filter { next in it.dependencies }.forEach { queue.add(it.name) }
            }
        }
        return dependents
    }
}
