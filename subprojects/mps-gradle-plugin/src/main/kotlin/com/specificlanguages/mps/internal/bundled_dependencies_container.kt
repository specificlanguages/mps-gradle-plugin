package com.specificlanguages.mps.internal

import com.specificlanguages.mps.BundledDependency
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Transformer
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import org.gradle.kotlin.dsl.register
import kotlin.collections.find

internal fun createBundledDependenciesContainer(
    objects: ObjectFactory,
    tasks: TaskContainer,
    configurations: ConfigurationContainer
): NamedDomainObjectContainer<BundledDependency> = objects.domainObjectContainer(
        BundledDependency::class.java,
        BundledDependencyFactory(configurations, tasks, objects)
    )

private class BundledDependencyFactory(
    private val configurations: ConfigurationContainer,
    private val tasks: TaskContainer,
    private val objects: ObjectFactory
) : NamedDomainObjectFactory<BundledDependency> {

    override fun create(name: String): BundledDependency {
        val configuration = configurations.register(name)
        val syncTask = tasks.register("resolve" + capitalize(name), Sync::class)
        val bd = objects.newInstance(BundledDependency::class.java, name, configuration, syncTask)

        configuration.configure {
            isCanBeConsumed = false
            fromDependencyCollector(bd.dependency)
        }

        syncTask.configure {
            description = "Downloads the '${bd.name}' bundled dependencies into their destination directory."
            from(configuration)
            into(bd.destinationDir)
            rename(VersionStrippingTransformer(bd.configuration.map { it.incoming.artifacts }))
            group = "build setup"
        }

        return bd
    }
}

private class VersionStrippingTransformer(val artifacts: Provider<ArtifactCollection>) : Transformer<String?, String> {
    override fun transform(filename: String): String? {
        val ra = artifacts.get().artifacts.find { ra -> ra.file.name == filename }!!
        val id = ra.id
        if (id is DefaultModuleComponentArtifactIdentifier && id.name.classifier != null) {
            // Artifact has a classifier, need to include it in the name
            return "${id.name.name}-${id.name.classifier}.${id.name.extension}"
        }

        val componentIdentifier = id.componentIdentifier
        if (componentIdentifier is ModuleComponentIdentifier) {
            return "${componentIdentifier.module}.${ra.file.extension}"
        }

        return filename
    }
}
