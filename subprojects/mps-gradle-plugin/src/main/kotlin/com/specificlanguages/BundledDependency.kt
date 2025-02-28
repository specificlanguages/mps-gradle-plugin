package com.specificlanguages

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.project

abstract class BundledDependency(private val name: String) : Named {
    override fun getName(): String = name

    abstract val destinationDir: DirectoryProperty

    abstract val dependency: DependencyCollector

    val configurationName: String = name
    abstract val configuration: Property<Configuration>

    abstract val syncTask: Property<Sync>
}
