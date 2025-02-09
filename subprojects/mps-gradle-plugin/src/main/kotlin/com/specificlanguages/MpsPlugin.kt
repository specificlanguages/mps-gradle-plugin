package com.specificlanguages

import com.specificlanguages.mps.ArtifactTransforms
import de.itemis.mps.gradle.launcher.MpsBackendLauncher
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
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

private fun stripVersionsAccordingToConfig(config: Provider<Configuration>): Transformer<String?, String> =
    Transformer { filename ->
        val ra = config.get().resolvedConfiguration.resolvedArtifacts.find { ra -> ra.file.name == filename }!!
        if (ra.classifier != null) {
            "${ra.name}-${ra.classifier}.${ra.extension}"
        } else {
            "${ra.name}.${ra.extension}"
        }
    }

private fun capitalize(s: String): String = s[0].uppercaseChar() + s.substring(1)

@Suppress("unused", "DEPRECATION")
open class MpsPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory,
    private val toolchains: JavaToolchainService
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(BasePlugin::class)
            pluginManager.apply(JavaBasePlugin::class)
            pluginManager.apply(ArtifactTransforms::class)

            val stubs = objects.domainObjectContainer(StubConfiguration::class)
            extensions.add(typeOf<NamedDomainObjectContainer<StubConfiguration>>(), "stubs", stubs)

            val syncTasks = objects.listProperty<TaskProvider<Sync>>()

            stubs.all {
                val stub = this
                val config = stub.configuration
                val task = tasks.register("resolve" + capitalize(stub.name), Sync::class) {
                    description = "Downloads dependencies of stub configuration '${stub.name}'."
                    from(config)
                    into(stub.destinationDir)
                    rename(stripVersionsAccordingToConfig(config))
                    group = "build setup"
                }

                syncTasks.add(task)
            }

            // Extend clean task to delete directories with MPS-generated files: source_gen, source_gen.caches,
            // classes_gen, tests_gen, tests_gen.caches.
            tasks.named(BasePlugin.CLEAN_TASK_NAME, Delete::class) {
                delete({ allGeneratedDirs(projectDir).asIterable() })
            }

            val generationConfiguration = configurations.create("generation") {
                isCanBeResolved = true
                isCanBeConsumed = false
            }

            // Set type of all artifacts to "zip" by default
            generationConfiguration.dependencies.withType(ModuleDependency::class).configureEach {
                artifact { type = "zip" }
            }

            val mpsConfiguration = configurations.create("mps") {
                isCanBeResolved = true
                isCanBeConsumed = false
            }

            val mpsDefaults = extensions.create<MpsDefaultsExtension>("mpsDefaults").apply {
                mpsHome.convention(layout.dir(ArtifactTransforms.getMpsRoot(mpsConfiguration)))

                buildScript.convention(layout.projectDirectory.file("build.xml"))

                dependenciesDirectory.convention(layout.buildDirectory.dir("dependencies"))

                javaExecutable.convention(javaLauncher.map { it.executablePath })
                javaLauncher.convention(toolchains.launcherFor {  })
            }

            syncTasks.add(tasks.register("resolveGenerationDependencies", Sync::class) {
                dependsOn(generationConfiguration)
                from({ generationConfiguration.resolve().map(project::zipTree) })
                into(mpsDefaults.dependenciesDirectory)
                group = "build setup"
                description = "Downloads and unpacks dependencies of '${generationConfiguration.name}' configuration."
            })

            val setupTask = tasks.register("setup", Sync::class) {
                dependsOn(syncTasks)
                group = "build setup"
                description = "Sets up the project so that it can be opened in MPS."
            }

            // Deprecated, remove in 2.0
            tasks.register("resolveMpsForGeneration") {
                doLast {
                    logger.warn("${name} is deprecated since mps-gradle-plugin version 1.4.0 and will be removed in version 2")
                }
            }

            val buildModel = findBuildModel(this)

            val executeGeneratorsConfiguration = configurations.create("executeGenerators") {
                defaultDependencies {
                    add(project.dependencies.create("de.itemis.mps.build-backends:execute-generators:[1.0,2.0)"))
                }
            }

            val generateBuildscriptTask = maybeRegisterGenerateBuildscriptTask(
                project,
                executeGeneratorsConfiguration,
                buildModel,
                mpsDefaults,
                setupTask
            )

            val antConfig = configurations.register("ant") {
                defaultDependencies {
                    add(project.dependencies.create("org.apache.ant:ant-junit:1.10.12"))
                }
            }

            val artifactsDir = layout.buildDirectory.dir("artifacts")

            tasks.withType(RunAntScript::class) {
                buildScript.convention(mpsDefaults.buildScript)
                javaExecutable.convention(mpsDefaults.javaExecutable)
                antProperties.convention(project.provider {
                    mapOf("mps_home" to mpsDefaults.mpsHome.get().asFile.path,
                        "version" to project.version.toString())
                })
                classpath.convention(antConfig)
            }

            val assembleMps = tasks.register("assembleMps", RunAntScript::class) {
                dependsOn(generateBuildscriptTask ?: setupTask)
                group = "build"
                description = "Assembles the MPS project."

                targets.set(listOf("generate", "assemble"))

                inputs.files(projectDirExcludingBuildDir(project).include("**/*.mps"))
                    .ignoreEmptyDirectories()
                    .withPropertyName("models")
                outputs.dir(artifactsDir)
            }

            val packagePluginZip = tasks.register("package", Zip::class) {
                description = "Packages the built modules in a ZIP archive."
                group = "build"

                from(assembleMps)
            }
            tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) { dependsOn(assembleMps) }

            val checkMps = tasks.register("checkMps", RunAntScript::class) {
                dependsOn(assembleMps)
                group = "build"
                description = "Runs tests in the MPS project."

                targets.set(listOf("check"))
            }
            tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(checkMps) }

            val defaultConfiguration = configurations["default"].apply {
                extendsFrom(generationConfiguration)
                outgoing.artifact(packagePluginZip)
                isCanBeConsumed = true
                isCanBeResolved = false

                // Add an attribute to keep Gradle happy ("variant must have at least one attribute")
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
            }

            val mpsComponent = softwareComponentFactory.adhoc("mps")
            mpsComponent.addVariantsFromConfiguration(defaultConfiguration) {
                mapToMavenScope("compile")
            }
            components.add(mpsComponent)
        }
    }

    private fun projectDirExcludingBuildDir(project: Project): ConfigurableFileTree {
        val projectDir = project.projectDir
        val projectTree = project.fileTree(projectDir)

        val buildDir = project.layout.buildDirectory.get().asFile
        if (buildDir.startsWith(projectDir)) {
            projectTree.exclude(buildDir.relativeTo(projectDir).path)
        }

        return projectTree
    }

    private fun maybeRegisterGenerateBuildscriptTask(
        project: Project,
        generateBackendConfiguration: Configuration,
        buildModel: File?,
        mpsDefaults: MpsDefaultsExtension,
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

            return tasks.register("generateBuildscript", JavaExec::class) {
                dependsOn(setupTask)
                args(
                    "--project=${projectDir}",
                    "--model=$buildModelName",
                    "--environment=MPS"
                )
                group = "build"
                description = "Generate the Ant build script from ${buildModel.relativeToOrSelf(projectDir)}."

                classpath(fileTree(mpsDefaults.mpsHome.map { it.dir("lib") }).include("**/*.jar"))
                classpath(fileTree(mpsDefaults.mpsHome.map { it.dir("plugins") }).include("**/lib/**/*.jar"))
                classpath(generateBackendConfiguration)

                mainClass.set("de.itemis.mps.gradle.generate.MainKt")

                inputs.file(buildModel).withPropertyName("build-model")
                inputs.files(projectDirExcludingBuildDir(project).include("**/*.msd", "**/*.mpl", "**/*.devkit"))
                    .ignoreEmptyDirectories()
                    .withPropertyName("module-files")
                outputs.file(mpsDefaults.buildScript)

                // Needed to avoid "URI is not hierarchical" exceptions
                environment("NO_FS_ROOTS_ACCESS_CHECK", "true")

                MpsBackendLauncher(project.objects).builder()
                    .withJavaExecutable(mpsDefaults.javaExecutable.map { it.asFile.path })
                    .withMpsHome(mpsDefaults.mpsHome.asFile)
                    .configure(this)
            }
        }
    }
}
