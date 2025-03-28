package com.specificlanguages.mps

import com.specificlanguages.jbrtoolchain.JbrToolchainExtension
import com.specificlanguages.jbrtoolchain.JbrToolchainPlugin
import com.specificlanguages.mps.internal.ConfigurationNames
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
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
            pluginManager.apply(BasePlugin::class.java)
            pluginManager.apply(ArtifactTransforms::class.java)
            pluginManager.apply(JbrToolchainPlugin::class.java)

            val mpsConfiguration = registerMpsConfiguration(configurations)
            val mpsDefaults = registerMpsDefaultsExtension(extensions, layout, mpsConfiguration)
            val bundledDependencies = registerBundledDependenciesExtension(tasks, configurations, objects, extensions)

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

            val setupTask = tasks.register("setup", Sync::class) {
                dependsOn(resolveMpsLibraries)
                dependsOn(Callable { bundledDependencies.map(BundledDependency::syncTask) })

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
            val mpsBuilds = registerMpsBuildsExtension(extensions, tasks, objects, layout, generateBuildScriptsTask)

            generateBuildScriptsTask.configure {
                dependsOn(setupTask)
                buildSolutionDescriptors.from(Callable { mpsBuilds.map(MpsBuild::buildSolutionDescriptor) })
            }

            registerGroupingTasks(tasks, mpsBuilds)

            // Extend clean task to delete directories with MPS-generated files: source_gen, source_gen.caches,
            // classes_gen, tests_gen, tests_gen.caches.
            tasks.named(BasePlugin.CLEAN_TASK_NAME, Delete::class) {
                delete({ allGeneratedDirs(layout.projectDirectory).asIterable() })
            }

            val packageTask = registerPackageTask(mpsBuilds, this.tasks)

            val apiElementsConfiguration =
                registerApiElementsConfiguration(apiConfiguration, packageTask, configurations, objects)

            configurations[Dependency.DEFAULT_CONFIGURATION].apply {
                extendsFrom(apiElementsConfiguration.get())
                outgoing.artifact(packageTask)
            }

            registerMpsComponent(components, apiElementsConfiguration)
        }
    }

    private fun registerApiElementsConfiguration(
        apiConfiguration: Provider<out Configuration>,
        packageTask: TaskProvider<Zip>,
        configurations: ConfigurationContainer,
        objects: ObjectFactory
    ): NamedDomainObjectProvider<ConsumableConfiguration> = configurations.consumable(ConfigurationNames.API_ELEMENTS) {
        extendsFrom(apiConfiguration.get())
        this.outgoing.artifact(packageTask)
        this.attributes.attribute<Usage>(
            Usage.USAGE_ATTRIBUTE,
            objects.named(Usage::class, Usage.JAVA_API)
        )
    }

    private fun registerPackageTask(
        mpsBuilds: ExtensiblePolymorphicDomainObjectContainer<MpsBuild>,
        tasks: TaskContainer
    ): TaskProvider<Zip> {
        val packageTask = tasks.register("package", Zip::class) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Packages the artifacts of all main MPS builds into a ZIP archive."

            mpsBuilds.withType(MainBuild::class).forEach {
                dependsOn(it.assembleTask)

                into(it.buildArtifactsDirectory.asFile.map(File::getName)) {
                    from(it.buildArtifactsDirectory)
                }
            }
        }
        return packageTask
    }

    private fun registerGroupingTasks(
        tasks: TaskContainer,
        mpsBuilds: ExtensiblePolymorphicDomainObjectContainer<MpsBuild>
    ) {
        tasks.register("generateMps") {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Runs 'generate' tasks of all MPS builds."
            dependsOn(Callable { mpsBuilds.map { it.generateTask } })
        }

        tasks.register("assembleMps") {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Runs 'assemble' tasks of all MPS main builds."

            dependsOn(Callable { mpsBuilds.withType(MainBuild::class.java).map { it.assembleTask } })
        }

        val testMps = tasks.register("testMps") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Runs 'check' tasks of all MPS test builds."

            dependsOn(Callable { mpsBuilds.withType(TestBuild::class.java).map { it.assembleAndCheckTask } })
        }

        val test = tasks.register("test") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Runs all tests in the project."
            dependsOn(testMps)
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

    private fun registerMpsBuildsExtension(
        extensions: ExtensionContainer,
        tasks: TaskContainer,
        objects: ObjectFactory,
        layout: ProjectLayout,
        generateBuildScriptsTask: TaskProvider<out Task>
    ): ExtensiblePolymorphicDomainObjectContainer<MpsBuild> {
        val mpsBuilds = objects.polymorphicDomainObjectContainer(MpsBuild::class.java)
        mpsBuilds.registerBinding(MainBuild::class.java, MainBuild::class.java)
        mpsBuilds.registerBinding(TestBuild::class.java, TestBuild::class.java)
        extensions.add(typeOf<PolymorphicDomainObjectContainer<MpsBuild>>(), "mpsBuilds", mpsBuilds)

        mpsBuilds.all {
            configureTasks(this, tasks, layout, generateBuildScriptsTask)
        }

        return mpsBuilds
    }

    private fun configureTasks(
        build: MpsBuild,
        tasks: TaskContainer,
        layout: ProjectLayout,
        generateBuildScriptsTask: TaskProvider<out Task>
    ) {
        build.generateTask.assign(tasks.register("generate${capitalize(build.name)}", RunAnt::class.java) {
            group = "build"
            description = "Runs 'generate' target of the '${build.name}' build."
            dependsOn(generateBuildScriptsTask)

            buildFile = build.buildFile
            targets.set(listOf("generate"))

            dependsOn(build.dependencies.map { it.map(MainBuild::assembleTask) })
        })

        when (build) {
            is MainBuild -> {
                build.assembleTask.assign(tasks.register("assemble${capitalize(build.name)}", RunAnt::class.java) {
                    group = "build"
                    description = "Runs 'assemble' target of the '${build.name}' build."
                    dependsOn(build.generateTask)

                    buildFile = build.buildFile
                    targets.set(listOf("assemble"))
                })
            }

            is TestBuild -> {
                build.assembleAndCheckTask.assign(tasks.register("check${capitalize(build.name)}", RunAnt::class.java) {
                    group = "build"
                    description = "Runs 'check' target of the '${build.name}' build."
                    dependsOn(build.generateTask)

                    buildFile = build.buildFile
                    targets.set(listOf("check"))
                })
            }
        }
    }

    private fun registerBundledDependenciesExtension(
        tasks: TaskContainer,
        configurations: ConfigurationContainer,
        objects: ObjectFactory,
        extensions: ExtensionContainer
    ): NamedDomainObjectContainer<BundledDependency> {
        val bundledDependencies = objects.domainObjectContainer(BundledDependency::class)
        extensions.add(
            typeOf<NamedDomainObjectContainer<BundledDependency>>(), "bundledDependencies",
            bundledDependencies
        )

        bundledDependencies.all {
            val bd = this

            bd.configuration = configurations.register(configurationName) {
                isCanBeConsumed = false
                fromDependencyCollector(bd.dependency)
            }

            bd.syncTask = tasks.register("resolve" + capitalize(bd.name), Sync::class) {
                description = "Downloads dependencies of stub configuration '${bd.name}'."
                from(bd.configuration)
                into(bd.destinationDir)
                rename(stripVersionsAccordingToConfig(bd.configuration))
                group = "build setup"
            }
        }

        return bundledDependencies
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
            pathProperties.putAll(mpsDefaults.pathVariables)

            valueProperties.put("version", providers.provider { project.version.toString() })
            classpath.convention(mpsDefaults.antClasspath)
        }

        tasks.withType(GenerateBuildScripts::class.java).configureEach {
            projectDirectory.convention(project.layout.projectDirectory)
            javaLauncher.convention(mpsDefaults.javaLauncher)
            generateBackendClasspath.convention(generateBackendConfiguration)
            mpsHome.convention(mpsDefaults.mpsHome)

            pathVariables.putAll(mpsDefaults.pathVariables)
        }
    }
}