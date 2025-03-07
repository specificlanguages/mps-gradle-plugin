package com.specificlanguages.mps

import org.gradle.api.Named
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Sync

abstract class BundledDependency(private val name: String) : Named {
    override fun getName(): String = name

    abstract val destinationDir: DirectoryProperty

    abstract val dependency: DependencyCollector

    val configurationName: String = name
    abstract val configuration: Property<Configuration>

    abstract val syncTask: Property<Sync>
}