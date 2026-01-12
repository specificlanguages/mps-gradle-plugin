package com.specificlanguages.mps

import com.specificlanguages.jbrtoolchain.JbrToolchainExtension
import com.specificlanguages.jbrtoolchain.JbrToolchainPlugin
import com.specificlanguages.mps.internal.ConfigurationNames
import com.specificlanguages.mps.internal.createBundledDependenciesContainer
import com.specificlanguages.mps.internal.createMpsBuildsContainer
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

private fun allGeneratedDirs(root: Directory): Sequence<File> {
    val dirsToFind = arrayOf("source_gen", "source_gen.caches", "classes_gen", "tests_gen", "tests_gen.caches")
    return sequence {
        val stack = mutableListOf<File>()
        stack.add(root.asFile)

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

@Suppress("unused", "DEPRECATION")
open class MpsPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory,
    private val toolchains: JavaToolchainService
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(BasePlugin::class.java)
            pluginManager.apply(ArtifactTransforms::class.java)
            pluginManager.apply(JbrToolchainPlugin::class.java)

            val mpsConfiguration = registerMpsConfiguration(configurations)
            val mpsDefaults = registerMpsDefaultsExtension(extensions, layout, mpsConfiguration)

            val bundledDependencies = createBundledDependenciesContainer(objects, tasks, configurations)
            extensions.add(
                typeOf<NamedDomainObjectContainer<BundledDependency>>(), "bundledDependencies",
                bundledDependencies
            )

            val apiConfiguration = configurations.dependencyScope(ConfigurationNames.API)

            val testImplementationConfiguration =
                configurations.dependencyScope(ConfigurationNames.TEST_IMPLEMENTATION) {
                    extendsFrom(apiConfiguration.get())
                }

            val mpsLibraries = configurations.resolvable(ConfigurationNames.MPS_LIBRARIES) {
                extendsFrom(apiConfiguration.get(), testImplementationConfiguration.get())
            }

            val resolveMpsLibraries by tasks.registering(Sync::class) {
                dependsOn(mpsLibraries)
                from(mpsLibraries.map { cfg -> cfg.map(project::zipTree) })

                into(mpsDefaults.mpsLibrariesDirectory)
                group = "build setup"
                description = "Downloads and extracts all external MPS libraries."
            }

            val setupTask = tasks.register("setup") {
                dependsOn(resolveMpsLibraries)
                dependsOn(Callable { bundledDependencies.map(BundledDependency::resolveTask) })

                group = "build setup"
                description = "Sets up the project so that it can be opened in MPS."
            }

            val executeGeneratorsConfiguration = configurations.register(ConfigurationNames.EXECUTE_GENERATORS) {
                isCanBeConsumed = false

                defaultDependencies {
                    add(project.dependencies.create("de.itemis.mps.build-backends:execute-generators:[1.0,2.0)"))
                }
            }

            configureTaskDefaults(tasks, providers, mpsDefaults, executeGeneratorsConfiguration)

            val generateBuildScriptsTask = tasks.register("generateBuildScripts", GenerateBuildScripts::class.java)

            val mpsBuilds = createMpsBuildsContainer(objects, tasks, generateBuildScriptsTask)
            extensions.add(typeOf<PolymorphicDomainObjectContainer<MpsBuild>>(), "mpsBuilds", mpsBuilds)

            generateBuildScriptsTask.configure {
                dependsOn(setupTask)
                buildSolutionDescriptorsByProject.putAll(provider {
                    mpsBuilds.groupBy({ it.mpsProjectDirectory.get() }, { it.buildSolutionDescriptor.get() })
                })
            }

            configureLifecycleTasks(tasks, mpsBuilds)

            // Extend the clean task to delete directories with MPS-generated files: source_gen, source_gen.caches,
            // classes_gen, tests_gen, tests_gen.caches.
            tasks.named(BasePlugin.CLEAN_TASK_NAME, Delete::class) {
                delete({ allGeneratedDirs(layout.projectDirectory).asIterable() })
            }

            val zipTask = registerZipTask(mpsBuilds, this.tasks)

            val apiElementsConfiguration =
                registerApiElementsConfiguration(apiConfiguration, zipTask, configurations, objects)

            configurations[Dependency.DEFAULT_CONFIGURATION].apply {
                extendsFrom(apiElementsConfiguration.get())
                outgoing.artifact(zipTask)
            }

            registerMpsComponent(components, apiElementsConfiguration)
        }
    }

    private fun registerApiElementsConfiguration(
        apiConfiguration: Provider<out Configuration>,
        zipTask: TaskProvider<Zip>,
        configurations: ConfigurationContainer,
        objects: ObjectFactory
    ): NamedDomainObjectProvider<ConsumableConfiguration> = configurations.consumable(ConfigurationNames.API_ELEMENTS) {
        extendsFrom(apiConfiguration.get())
        this.outgoing.artifact(zipTask)
        this.attributes.attribute(
            Usage.USAGE_ATTRIBUTE,
            objects.named(Usage::class, Usage.JAVA_API)
        )
    }

    private fun registerZipTask(
        mpsBuilds: DomainObjectCollection<MpsBuild>,
        tasks: TaskContainer
    ): TaskProvider<Zip> {
        val task = tasks.register("zip", Zip::class.java) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Packages the artifacts of all main published MPS builds into a ZIP archive."

            fun addToZipIfPublished(build: MainBuild) {
                val task = this@register
                task.dependsOn(build.assembleTask)

                task.into(build.buildArtifactsDirectory.asFile.map(File::getName)) {
                    from(build.buildArtifactsDirectory)
                    exclude { !build.published.get() }
                }
            }

            mpsBuilds.withType(MainBuild::class.java).all { addToZipIfPublished(this) }
        }
        return task
    }

    private fun configureLifecycleTasks(
        tasks: TaskContainer,
        mpsBuilds: DomainObjectCollection<MpsBuild>
    ) {
        tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(Callable { mpsBuilds.withType(MainBuild::class.java).map { it.assembleTask } })
        }

        val test = tasks.register("test") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Runs 'check' tasks of all MPS test builds."

            dependsOn(Callable { mpsBuilds.withType(TestBuild::class.java).map { it.assembleAndCheckTask } })
        }

        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(test) }
    }

    private fun registerMpsComponent(
        components: SoftwareComponentContainer,
        apiElements: Provider<out Configuration>
    ) {
        val mpsComponent = softwareComponentFactory.adhoc("mps")
        mpsComponent.addVariantsFromConfiguration(apiElements.get()) { mapToMavenScope("compile") }
        components.add(mpsComponent)
    }

    private fun registerMpsConfiguration(configurations: ConfigurationContainer): NamedDomainObjectProvider<Configuration> =
        configurations.register(ConfigurationNames.MPS) {
            isCanBeResolved = true
            isCanBeConsumed = false

            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTransforms.UNZIP_MPS_FROM_ARTIFACT_TYPE
            )
        }

    private fun registerMpsDefaultsExtension(
        extensions: ExtensionContainer,
        layout: ProjectLayout,
        mpsConfiguration: Provider<out Configuration>
    ): MpsDefaultsExtension =
        extensions.create<MpsDefaultsExtension>("mpsDefaults").apply {
            mpsHome.convention(layout.dir(mpsConfiguration.flatMap(ArtifactTransforms::getMpsRoot)))
            mpsLibrariesDirectory.convention(layout.buildDirectory.dir("dependencies"))
            javaLauncher.convention(extensions.getByType<JbrToolchainExtension>().javaLauncher)
            antClasspath.convention(mpsHome.dir("lib/ant/lib").map {
                it.asFileTree.matching {
                    include("*.jar")
                    // ant-mps.jar contains MpsLoadTask which depends on JDOM which is not found among the Ant
                    // libraries. If the jar is left on the classpath, Ant loads MpsLoadTask from its own classloader
                    // and does not use the classpath specified in the <taskdef> in the build script, leading to
                    // a NoClassDefFoundError.
                    exclude("ant-mps.jar")
                }
            })
        }

    private fun configureTaskDefaults(
        tasks: TaskContainer,
        providers: ProviderFactory,
        mpsDefaults: MpsDefaultsExtension,
        generateBackendConfiguration: NamedDomainObjectProvider<Configuration>
    ) {
        tasks.withType(RunAnt::class.java).configureEach {
            javaLauncher.convention(mpsDefaults.javaLauncher)
            pathProperties.put("mps_home", mpsDefaults.mpsHome.asFile)
            pathProperties.put("mps.home", mpsDefaults.mpsHome.asFile)
            pathProperties.put(
                "build.jna.library.path",
                mpsDefaults.mpsHome.zip(providers.systemProperty("os.arch")) { mpsHome, osArch ->
                    val jnaArch = when (osArch) {
                        "x86_64" -> "amd64"
                        else -> osArch
                    }

                    mpsHome.dir("lib/jna/$jnaArch").asFile }
            )
            pathProperties.put("build.mps.config.path", temporaryDir.resolve("config"))
            pathProperties.put("build.mps.system.path", temporaryDir.resolve("system"))
            pathProperties.put("mps.log.dir", temporaryDir.resolve("log"))

            pathProperties.putAll(mpsDefaults.pathVariables)

            valueProperties.put("version", providers.provider { project.version.toString() })
            classpath.convention(mpsDefaults.antClasspath)
        }

        tasks.withType(GenerateBuildScripts::class.java).configureEach {
            javaLauncher.convention(mpsDefaults.javaLauncher)
            generateBackendClasspath.convention(generateBackendConfiguration)
            mpsHome.convention(mpsDefaults.mpsHome)

            pathVariables.putAll(mpsDefaults.pathVariables)
        }
    }
}