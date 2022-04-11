package com.specificlanguages

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.*
import java.io.File
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

private fun allGeneratedDirs(root : File): Sequence<File> {
    val dirsToFind = arrayOf("source_gen", "source_gen.caches", "classes_gen", "tests_gen", "tests_gen.caches")
    return sequence {
        val stack = mutableListOf<File>()
        stack.add(root)

        while (stack.isNotEmpty()) {
            val top = stack.removeAt(stack.size - 1)
            val files = top.listFiles() ?: continue

            for (file in files) {
                if (!file.isDirectory) { continue }

                if (file.name in dirsToFind) {
                    yield(file)
                } else {
                    stack.add(file)
                }
            }
        }
    }
}

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

    fun dependency(notation: Any, config: Closure<*>): Dependency {
        return project.dependencies.add(configuration.name, notation, config)
    }

    fun dependency(notation: Any, config: (ExternalModuleDependency).() -> Unit): ExternalModuleDependency =
        dependency(notation, closureOf(config)) as ExternalModuleDependency

}

fun stripVersionsAccordingToConfig(config: Provider<Configuration>): Transformer<String, String> {
    return Transformer { filename ->
        val ra = config.get().resolvedConfiguration.resolvedArtifacts.find { ra -> ra.file.name == filename }!!
        if (ra.classifier != null) {
            "${ra.name}-${ra.classifier}.${ra.extension}"
        } else {
            "${ra.name}.${ra.extension}"
        }
    }
}

fun capitalize(s: String): String = s[0].toUpperCase() + s.substring(1)

@Suppress("unused")
open class MpsPlugin @Inject constructor(
    val objectFactory: ObjectFactory,
    val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(BasePlugin::class.java)

            val stubs = objectFactory.domainObjectContainer(StubConfiguration::class.java)
            extensions.add(typeOf<NamedDomainObjectContainer<StubConfiguration>>(), "stubs", stubs)

            val syncTasks = objects.listProperty<TaskProvider<Sync>>()

            stubs.all {
                val stub = this
                val config = stub.configuration
                val task = tasks.register("resolve" + capitalize(stub.name), Sync::class.java) {
                    description = "Downloads dependencies of stub configuration '${stub.name}'" +
                            " into ${stub.destinationDir.get().asFile.relativeToOrSelf(projectDir)}."
                    from(config)
                    into(stub.destinationDir)
                    rename(stripVersionsAccordingToConfig(config))
                    group = "build setup"
                }

                syncTasks.add(task)
            }

            // Extend clean task to delete directories with MPS-generated files: source_gen, source_gen.caches,
            // classes_gen, tests_gen, tests_gen.caches.
            tasks.named(BasePlugin.CLEAN_TASK_NAME, Delete::class.java) {
                delete({ allGeneratedDirs(projectDir).asIterable() })
            }

            val generationConfiguration = configurations.create("generation")
            generationConfiguration.isCanBeResolved = true
            generationConfiguration.isCanBeConsumed = false

            // Set type of all artifacts to "zip" by default
            generationConfiguration.dependencies.withType(ModuleDependency::class).configureEach {
                artifact { type = "zip" }
            }

            val mpsConfiguration = configurations.create("mps")
            mpsConfiguration.isCanBeResolved = true
            mpsConfiguration.isCanBeConsumed = false

            syncTasks.add(tasks.register("resolveGenerationDependencies", Sync::class.java) {
                dependsOn(generationConfiguration)
                from({ generationConfiguration.resolve().map(project::zipTree) })
                into("build/dependencies")
                group = "build setup"
                description = "Downloads dependencies of '${generationConfiguration.name}' configuration" +
                        " and unpacks them into ${destinationDir.relativeToOrSelf(projectDir)}."
            })

            val setupTask = tasks.register("setup", Sync::class.java) {
                dependsOn(syncTasks)
                group = "build setup"
                description = "Sets up the project so that it can be opened in MPS."
            }

            val distLocation = File(buildDir, "mps")
            val distResolveTask = registerMpsResolveTask(mpsConfiguration, distLocation)

            val buildModel = findBuildModel(this)

            val executeGeneratorsConfiguration = configurations.create("executeGenerators")
            executeGeneratorsConfiguration.withDependencies {
                if (isEmpty() && executeGeneratorsConfiguration.extendsFrom.isEmpty()) {
                    add(dependencies.create("de.itemis.mps.build-backends:execute-generators:[1.0,2.0)"))
                }
            }

            val generateBuildscriptTask = maybeRegisterGenerateBuildscriptTask(
                project, executeGeneratorsConfiguration, buildModel, distLocation, distResolveTask, setupTask)

            val antConfig = configurations.detachedConfiguration(
                    dependencies.create("org.apache.ant:ant-junit:1.10.12"))

            val artifactsDir = File(project.projectDir, "build/artifacts")

            val assembleMps = tasks.register("assembleMps", RunAntScript::class.java) {
                dependsOn(distResolveTask, generateBuildscriptTask ?: setupTask)
                group = "build"
                description = "Assembles the MPS project."
                script = "build.xml"
                inputs.files(fileTree(projectDir).include("**/*.mps")).withPropertyName("models")
                targets = listOf("generate", "assemble")
                scriptArgs = listOf("-Dmps_home=$distLocation", "-Dversion=${project.version}")
                scriptClasspath = antConfig
                outputs.dir(artifactsDir)
            }

            val packagePluginZip = tasks.register("package", Zip::class.java) {
                description = "Packages the built modules in a ZIP archive."
                group = "build"
                dependsOn(assembleMps)
                destinationDirectory.set(File(project.buildDir, "dist"))
                archiveBaseName.set(project.name)
                from(assembleMps)
            }
            tasks.named("assemble") { dependsOn(assembleMps) }

            val checkMps = tasks.register("checkMps", RunAntScript::class.java) {
                dependsOn(assembleMps)
                group = "build"
                description = "Runs tests in the MPS project."
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

            // Add an attribute to keep Gradle happy ("variant must have at least one attribute")
            defaultConfiguration.attributes.attribute(Usage.USAGE_ATTRIBUTE,
                project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))

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
            from({ mpsConfiguration.map(::zipTree) })
            into(distLocation)
            description = "Downloads MPS as specified by '${mpsConfiguration.name}' configuration" +
                    " and unpacks it into ${distLocation.relativeToOrSelf(projectDir)}."
            group = "build setup"
        }
    }

    private fun maybeRegisterGenerateBuildscriptTask(
        project: Project,
        generateBackendConfiguration: Configuration,
        buildModel: File?,
        distLocation: File,
        distResolveTask: TaskProvider<Sync>,
        setupTask: Any
    ): TaskProvider<JavaExec>? {
        if (buildModel == null) {
            return null
        }

        project.run {
            logger.info("Using build model {}", buildModel)
            val buildModelName = readModelName(buildModel) ?: throw GradleException(
                "Could not retrieve build model name from model $buildModel"
            )

            return project.tasks.register("generateBuildscript", JavaExec::class.java) {
                dependsOn(distResolveTask, setupTask)
                args(
                    "--project=${projectDir}",
                    "--model=$buildModelName"
                )
                group = "build"
                description = "Generate the Ant build script from ${buildModel.relativeToOrSelf(projectDir)}."
                classpath(fileTree(File(distLocation, "lib")).include("**/*.jar"))
                classpath(fileTree(File(distLocation, "plugins")).include("**/lib/**/*.jar"))
                classpath(generateBackendConfiguration)

                mainClass.set("de.itemis.mps.gradle.generate.MainKt")

                inputs.file(buildModel).withPropertyName("build-model")
                inputs.files(fileTree(this.project.projectDir).include("**/*.msd", "**/*.mpl", "**/*.devkit"))
                    .withPropertyName("module-files")
                outputs.file("build.xml")

                // Needed to avoid "URI is not hierarchical" exceptions
                environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
            }
        }
    }

}
