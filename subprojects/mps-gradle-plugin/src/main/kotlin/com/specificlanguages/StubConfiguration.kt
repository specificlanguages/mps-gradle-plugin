package com.specificlanguages

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.project

abstract class StubConfiguration(private val project: Project, private val name: String) : Named {
    override fun getName(): String = name

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Input
    val configuration: NamedDomainObjectProvider<Configuration> = project.configurations.register(name) { isCanBeConsumed = false }

    fun destinationDir(path: Any) {
        destinationDir.set(project.file(path))
    }

    fun dependency(notation: Any): Dependency? {
        return project.dependencies.add(configuration.name, notation)
    }

    fun dependency(notation: Any, config: Closure<*>): Dependency? {
        return project.dependencies.add(configuration.name, notation, config)
    }

    fun dependency(notation: String, config: (ExternalModuleDependency).() -> Unit): ExternalModuleDependency =
        dependency(notation, closureOf(config)) as ExternalModuleDependency

    fun project(path: String, configuration: String? = null): ProjectDependency =
        project.dependencies.project(path, configuration)
    fun project(notation: Map<String, Any>) = project.dependencies.project(notation)

    fun <T : ModuleDependency> dependency(dependency: T, config: T.() -> Unit): T =
        project.dependencies.add(configuration.name, dependency, config)
}
