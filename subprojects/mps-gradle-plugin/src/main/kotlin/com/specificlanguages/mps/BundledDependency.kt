package com.specificlanguages.mps

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Sync
import javax.inject.Inject

abstract class BundledDependency @Inject constructor(
    private val name: String,
    val configuration: NamedDomainObjectProvider<Configuration>,
    val syncTask: NamedDomainObjectProvider<Sync>
) : Named {
    override fun getName(): String = name

    abstract val destinationDir: DirectoryProperty

    abstract val dependency: DependencyCollector
}
