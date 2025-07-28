package com.specificlanguages.mps.internal

import com.specificlanguages.mps.BundledDependency
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.register

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
            rename(stripVersionsAccordingToConfig(bd.configuration))
            group = "build setup"
        }

        return bd
    }
}

private fun stripVersionsAccordingToConfig(config: Provider<Configuration>): Transformer<String?, String> =
    Transformer { filename ->
        val ra = config.get().resolvedConfiguration.resolvedArtifacts.find { ra -> ra.file.name == filename }!!
        if (ra.classifier != null) {
            "${ra.name}-${ra.classifier}.${ra.extension}"
        } else {
            "${ra.name}.${ra.extension}"
        }
    }
