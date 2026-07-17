import com.specificlanguages.buildlogic.CheckReleaseVersionsTask
import com.specificlanguages.buildlogic.ModuleInfo
import org.gradle.api.artifacts.ProjectDependency

evaluationDependsOnChildren()

val moduleInfos = subprojects.map { subproject ->
    val dependencies = listOf("api", "implementation")
        .mapNotNull { subproject.configurations.findByName(it) }
        .flatMap { it.dependencies.withType(ProjectDependency::class.java).map(ProjectDependency::getName) }
        .toSet()
    ModuleInfo(
        name = subproject.name,
        version = subproject.version.toString(),
        path = rootDir.toPath().relativize(subproject.projectDir.toPath()).toString().replace('\\', '/'),
        dependencies = dependencies
    )
}

// Fails a release if a changed module's dependents have not also been bumped. Runs the per-module API
// compatibility checks too, so this single task covers version-policy validation. Not wired into `check`:
// it needs full git history and is meant for release preparation and CI.
tasks.register<CheckReleaseVersionsTask>("checkReleaseVersions") {
    group = "verification"
    description = "Checks that modules depending on a changed module are bumped, and that API changes are versioned."
    modules.set(moduleInfos)
    repositoryRoot.set(layout.projectDirectory)
    dependsOn(subprojects.map { "${it.path}:checkApiCompatibility" })
}
