package com.specificlanguages

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.support.zipTo
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject

private fun findBuildModel(project: Project): File? =
        project.projectDir.walkBottomUp().firstOrNull { it.name == "build.mps" || it.name.endsWith(".build.mps") }

private val MODEL_NAME_REGEX = Regex("""<model ref=".*\((.*)\)">""")

private fun readModelName(file: File): String? = file.bufferedReader(Charsets.UTF_8).use {
    val xmlHeader = it.readLine()
    if (!"<?xml version=\"1.0\" encoding=\"UTF-8\"?>".equals(xmlHeader, true)) {
        return null
    }
    val modelHeader = it.readLine()
    val matchResult = MODEL_NAME_REGEX.find(modelHeader) ?: return null
    return matchResult.groupValues[1]
}

@Suppress("unused", "UnstableApiUsage")
open class MpsPlugin @Inject constructor(val softwareComponentFactory: SoftwareComponentFactory) : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(BasePlugin::class.java)

            val generationConfiguration = configurations.create("generation")
            generationConfiguration.isCanBeResolved = true
            generationConfiguration.isCanBeConsumed = false

            val mpsConfiguration = configurations.create("mps")
            mpsConfiguration.isCanBeResolved = true
            mpsConfiguration.isCanBeConsumed = false

            val setupTask = tasks.register("setup", Sync::class.java) {
                dependsOn(generationConfiguration)
                from({ generationConfiguration.resolve().map { project.zipTree(it) } })
                into("build/dependencies")

                group = "build setup"
                description = "Download dependencies into ${destinationDir.relativeToOrSelf(projectDir)}"
            }

            val distLocation = File(buildDir, "mps")
            val distResolveTask = registerMpsResolveTask(mpsConfiguration, distLocation)

            val buildModel = findBuildModel(this)

            val generateBuildscriptTask = project.maybeRegisterGenerateBuildscriptTask(
                    mpsConfiguration, buildModel, distLocation, distResolveTask, setupTask)

            val antConfig = configurations.detachedConfiguration(
                    dependencies.create("org.apache.ant:ant-junit:1.10.1"))

            val artifactsDir = File(project.projectDir, "build/artifacts")

            val assembleMps = tasks.register("assembleMps", RunAntScript::class.java) {
                dependsOn(distResolveTask, generateBuildscriptTask ?: setupTask)
                group = "build"
                description = "Assemble the MPS project"
                script = "build.xml"
                targets = listOf("generate", "assemble")
                scriptArgs = listOf("-Dmps_home=$distLocation")
                scriptClasspath = antConfig
                outputs.dir(artifactsDir)
            }

            val packagePluginZip = tasks.create("package", Zip::class.java) {
                description = "Package the built modules in a ZIP archive"
                dependsOn(assembleMps)
                destinationDirectory.set(File(project.buildDir, "dist"))
                archiveBaseName.set(project.name)
                from(assembleMps)
            }
            tasks.named("assemble") { dependsOn(assembleMps) }

            val checkMps = tasks.register("checkMps", RunAntScript::class.java) {
                dependsOn(assembleMps)
                group = "build"
                description = "Run tests in the MPS project"
                script = "build.xml"
                targets = listOf("check")
                scriptArgs = listOf("-Dmps_home=$distLocation")
                scriptClasspath = antConfig
            }
            tasks.named("check") { dependsOn(checkMps) }

            val defaultConfiguration = project.configurations["default"]
            defaultConfiguration.extendsFrom(generationConfiguration)
            defaultConfiguration.outgoing.artifact(packagePluginZip)
            defaultConfiguration.isCanBeConsumed = true
            defaultConfiguration.isCanBeResolved = false

            val mpsComponent = softwareComponentFactory.adhoc("mps")
            mpsComponent.addVariantsFromConfiguration(defaultConfiguration) {
                mapToMavenScope("compile")
            }
            components.add(mpsComponent)
        }
    }

    private fun Project.registerMpsResolveTask(mpsConfiguration: Configuration,
                                               distLocation: File): TaskProvider<Sync> {
        return tasks.register("resolveMpsForGeneration", Sync::class.java) {
            from({ mpsConfiguration.resolve().map(::zipTree) })
            into(distLocation)
        }
    }

    private fun Project.maybeRegisterGenerateBuildscriptTask(
            mpsConfiguration: Configuration,
            buildModel: File?,
            distLocation: File,
            distResolveTask: TaskProvider<Sync>,
            setupTask: Any): TaskProvider<JavaExec>? {
        if (buildModel == null) {
            return null
        }

        logger.info("Using build model {}", buildModel)
        val buildModelName = readModelName(buildModel) ?: throw GradleException(
                "Could not retrieve build model name from model $buildModel")

        return tasks.register("generateBuildscript", JavaExec::class.java) {
            dependsOn(distResolveTask, setupTask)
            args(
                    "--project=${projectDir}",
                    "--model=$buildModelName")
            group = "build"
            description = "Generate the Ant build script from " + buildModel.relativeTo(projectDir)
            classpath(fileTree(File(distLocation, "lib")).include("**/*.jar"))
            classpath(fileTree(File(distLocation, "plugins")).include("**/lib/**/*.jar"))

            val mpsVersion = getMpsVersion(mpsConfiguration)
            classpath({ configurations.detachedConfiguration(createExecuteGeneratorDependency(mpsVersion)) })

            main = "de.itemis.mps.gradle.generate.MainKt"

            inputs.file(buildModel)
            outputs.file("build.xml")

            // Needed to avoid "URI is not hierarchical" exceptions
            environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
        }
    }

    private fun getMpsVersion(mpsConfiguration: Configuration): String {
        val dependencies = mpsConfiguration.dependencies
        if (dependencies.size != 1) {
            throw GradleException("Expected configuration '${mpsConfiguration.name}' to contain exactly one" +
                    " dependency, the MPS to use. But found ${mpsConfiguration.dependencies.size} dependencies.")
        }

        return dependencies.first().version
                ?: throw GradleException("Expected configuration '${mpsConfiguration.name}' to contain exactly one" +
                        " dependency with a version. But the dependency has no version specified.")

    }

    private fun Project.createExecuteGeneratorDependency(mpsVersion: String): Dependency {
        val dep = dependencies.create("de.itemis.mps:execute-generators:${mpsVersion}.+")
        return dep
    }
}
