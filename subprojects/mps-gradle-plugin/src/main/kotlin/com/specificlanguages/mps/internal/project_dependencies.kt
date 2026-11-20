package com.specificlanguages.mps.internal

import com.specificlanguages.mps.MainBuild
import com.specificlanguages.mps.MpsBuild
import com.specificlanguages.mps.RunAnt
import com.specificlanguages.mps.TestBuild
import org.gradle.api.DomainObjectCollection
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.Date

private const val ARTIFACT_DIRECTORIES_VARIANT = "artifactDirectories"

/**
 * Adds a secondary variant to [apiElements] that exposes the artifacts directories of published main builds instead of
 * the zip. Secondary variants are only visible to other projects of the same build (or included builds) and are not
 * published.
 */
internal fun registerArtifactDirectoriesVariant(
    apiElements: NamedDomainObjectProvider<ConsumableConfiguration>,
    mpsBuilds: DomainObjectCollection<MpsBuild>,
    providers: ProviderFactory
) {
    apiElements.configure {
        outgoing.variants.create(ARTIFACT_DIRECTORIES_VARIANT) {
            attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
            artifacts.addAllLater(providers.provider {
                mpsBuilds.withType(MainBuild::class.java)
                    .filter { it.published.get() }
                    .map(::BuildArtifactsDirectory)
            })
        }
    }
}

/**
 * Passes the location of the artifacts of each main build that the Ant scripts of this project may depend on as
 * an `artifacts.<directory name>` Ant property. MPS-generated Ant scripts read the location of the artifacts of
 * a dependency build project named `X` from the `artifacts.X` property.
 *
 * Covers the main builds of this project and the published main builds of other projects in [mpsLibraries]. The Ant
 * tasks also depend on assembling the builds of the other projects.
 */
internal fun configureProjectDependencies(
    mpsBuilds: DomainObjectCollection<MpsBuild>,
    mpsLibraries: NamedDomainObjectProvider<ResolvableConfiguration>,
    providers: ProviderFactory
) {
    val otherProjectsArtifacts = mpsLibraries.map { cfg ->
        cfg.incoming.artifactView {
            componentFilter { it is ProjectComponentIdentifier }
            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTypeDefinition.DIRECTORY_TYPE
            )
        }.files
    }

    val thisProjectArtifacts = providers.provider {
        mpsBuilds.withType(MainBuild::class.java).mapNotNull { it.buildArtifactsDirectory.orNull?.asFile }
    }

    mpsBuilds.configureEach {
        antTasks(this).forEach { task ->
            task.configure {
                dependsOn(otherProjectsArtifacts)
                pathProperties.putAll(otherProjectsArtifacts.map { files -> artifactsProperties(files.files) })
                pathProperties.putAll(thisProjectArtifacts.map(::artifactsProperties))
            }
        }
    }
}

private fun antTasks(build: MpsBuild): List<TaskProvider<out RunAnt>> = when (build) {
    is MainBuild -> listOf(build.generateTask, build.assembleTask)
    is TestBuild -> listOf(build.generateTask, build.assembleAndCheckTask)
    else -> listOf(build.generateTask)
}

private fun artifactsProperties(directories: Iterable<File>): Map<String, File> =
    directories.associateBy { "artifacts.${it.name}" }

/**
 * The artifacts directory of a main build, built by its assemble task.
 *
 * `ConfigurationVariant.artifact(...)` would create and configure the artifact for us, but it adds the artifact
 * immediately, so it would have to be called once [MainBuild.published] is known. That is not the case in
 * `mpsBuilds.all { ... }`: `create(name) { ... }` adds a build to the container before running its configuration
 * block. Implementing [PublishArtifact] lets [registerArtifactDirectoriesVariant] add the artifacts lazily through
 * `addAllLater`. Gradle's own plugins use the internal `LazyPublishArtifact` class for the same purpose.
 */
private class BuildArtifactsDirectory(private val build: MainBuild) : PublishArtifact {
    override fun getName(): String = file.name
    override fun getExtension(): String = ""
    override fun getType(): String = ArtifactTypeDefinition.DIRECTORY_TYPE
    override fun getClassifier(): String? = null
    override fun getFile(): File = build.buildArtifactsDirectory.get().asFile
    override fun getDate(): Date? = null
    override fun getBuildDependencies(): TaskDependency = TaskDependency { setOf(build.assembleTask.get()) }
}
