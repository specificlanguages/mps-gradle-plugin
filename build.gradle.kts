import com.specificlanguages.buildlogic.CheckReleaseVersionsTask
import com.specificlanguages.buildlogic.ModuleInfo
import org.gradle.api.artifacts.ProjectDependency

evaluationDependsOnChildren()

val moduleInfos = subprojects.map { subproject ->
    val dependencies = listOf("api", "implementation", "runtimeOnly")
        .mapNotNull { subproject.configurations.findByName(it) }
        .flatMap { it.dependencies.withType<ProjectDependency>().map(ProjectDependency::getName) }
        .toSet()
    ModuleInfo(
        name = subproject.name,
        version = subproject.version.toString(),
        path = rootDir.toPath().relativize(subproject.projectDir.toPath()).toString().replace('\\', '/'),
        dependencies = dependencies
    )
}

// Fails a release if a changed module or its dependents have not been bumped. Runs the per-module API
// compatibility checks too, so this single task covers version-policy validation. Not wired into `check`:
// it needs full git history and is meant for release preparation and CI.
tasks.register<CheckReleaseVersionsTask>("checkReleaseVersions") {
    group = "verification"
    description = "Checks that changed modules and their dependents are bumped, and that API changes are versioned."
    modules = moduleInfos
    repositoryRoot = layout.projectDirectory
    dependsOn(subprojects.map { "${it.path}:checkApiCompatibility" })
}
