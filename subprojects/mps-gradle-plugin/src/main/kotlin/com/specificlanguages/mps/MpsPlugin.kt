package com.specificlanguages.mps

import com.specificlanguages.jbrtoolchain.JbrToolchainExtension
import com.specificlanguages.jbrtoolchain.JbrToolchainPlugin
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
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
            pluginManager.apply(BasePlugin::class)
            pluginManager.apply(ArtifactTransforms::class.java)
            pluginManager.apply(JbrToolchainPlugin::class.java)

            val mpsConfiguration = registerMpsConfiguration(configurations)
            val mpsHomeConfiguration = registerMpsHomeConfiguration(configurations, mpsConfiguration)
            val mpsDefaults = registerMpsDefaultsExtension(extensions, layout, mpsHomeConfiguration)
            val bundledDependencies = registerBundledDependenciesExtension(tasks, configurations, objects, extensions)

            val generationConfiguration = registerGenerationConfiguration(configurations)

            val resolveGenerationDependencies by tasks.registering(Sync::class) {
                from(generationConfiguration.map { cfg -> cfg.map(project::zipTree) })
                into(mpsDefaults.mpsLibrariesDirectory)
                group = "build setup"
                description = "Downloads and extracts all external MPS libraries."
            }

            val setupTask = tasks.register("setup", Sync::class) {
                dependsOn(resolveGenerationDependencies)
                dependsOn(Callable { bundledDependencies.map(BundledDependency::syncTask) })

                group = "build setup"
                description = "Sets up the project so that it can be opened in MPS."
            }

            val executeGeneratorsConfiguration = configurations.register("executeGenerators") {
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

            val defaultConfiguration = configurations["default"];

            configureDefaultConfiguration(defaultConfiguration, generationConfiguration, providers, objects, mpsBuilds)
            registerMpsComponentFromConfiguration(components, defaultConfiguration)
        }
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

        val packageMps = tasks.register("packageMps") {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Packages the artifacts of all main MPS modules into ZIP archives."

            dependsOn(Callable { mpsBuilds.withType(MainBuild::class.java).map { it.packageTask.get() } })
        }

        tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) { dependsOn(packageMps) }
    }

    private fun configureDefaultConfiguration(
        configuration: Configuration, generationConfiguration: Provider<Configuration>,
        providers: ProviderFactory,
        objects: ObjectFactory,
        mpsBuilds: PolymorphicDomainObjectContainer<MpsBuild>
    ) {
        configuration.extendsFrom(generationConfiguration.get())

        configuration.outgoing.artifacts(providers.provider {
            mpsBuilds.withType(MainBuild::class.java).map { it.packageTask }
        })

        configuration.isCanBeConsumed = true
        configuration.isCanBeResolved = false

        // Add an attribute to keep Gradle happy ("variant must have at least one attribute")
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))

    }

    private fun registerMpsComponentFromConfiguration(
        components: SoftwareComponentContainer,
        configuration: Configuration
    ) {
        val mpsComponent = softwareComponentFactory.adhoc("mps")
        mpsComponent.addVariantsFromConfiguration(configuration) {
            mapToMavenScope("compile")
        }
        components.add(mpsComponent)
    }

    private fun registerMpsConfiguration(configurations: ConfigurationContainer): NamedDomainObjectProvider<Configuration> =
        configurations.register("mps") {
            isCanBeResolved = true
            isCanBeConsumed = false

            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTransforms.UNZIP_MPS_FROM_ARTIFACT_TYPE
            )
        }

    private fun registerMpsHomeConfiguration(
        configurations: ConfigurationContainer,
        mpsConfiguration: NamedDomainObjectProvider<Configuration>
    ): NamedDomainObjectProvider<Configuration> =
        configurations.register("mpsHome") {
            isCanBeResolved = true
            isCanBeDeclared = false
            isCanBeConsumed = false

            attributes.attribute(
                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                ArtifactTransforms.UNZIP_MPS_TO_ARTIFACT_TYPE
            )

            extendsFrom(mpsConfiguration.get())
        }

    private fun registerGenerationConfiguration(configurations: ConfigurationContainer): NamedDomainObjectProvider<Configuration> =
        configurations.register("generation") {
            isCanBeResolved = true
            isCanBeConsumed = false
        }

    private fun checkSingleFileInMpsConfiguration(files: FileCollection): File {
        val configurationName = "mps"
        val iterator = files.iterator()

        check(iterator.hasNext()) {
            "Expected configuration '$configurationName' to contain exactly one file, however, it contains no files. " +
                    "Make sure you add a dependency on the appropriate version of MPS to the '$configurationName' " +
                    "configuration."
        }

        val singleFile = iterator.next()

        check(!iterator.hasNext()) {
            "Expected configuration '$configurationName' to contain exactly one file, however, it contains multiple " +
                    "files. Make sure you only add a single dependency to the '$configurationName' configuration."
        }

        return singleFile!!
    }


    private fun registerMpsDefaultsExtension(
        extensions: ExtensionContainer,
        layout: ProjectLayout,
        mpsHomeConfiguration: Provider<Configuration>
    ): MpsDefaultsExtension =
        extensions.create<MpsDefaultsExtension>("mpsDefaults").apply {
            mpsHome.convention(layout.dir(mpsHomeConfiguration.map(::checkSingleFileInMpsConfiguration)))
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
                build.artifactsDirectory.convention(layout.buildDirectory.dir(build.buildProjectName.map { "artifacts/$it" }))


                build.assembleTask.assign(tasks.register("assemble${capitalize(build.name)}", RunAnt::class.java) {
                    group = "build"
                    description = "Runs 'assemble' target of the '${build.name}' build."
                    dependsOn(build.generateTask)

                    buildFile = build.buildFile
                    targets.set(listOf("assemble"))
                })

                build.packageTask = tasks.register("package${capitalize(build.name)}", Zip::class) {
                    description = "Packages the artifacts of '${build.name}' build into a ZIP archive."
                    group = LifecycleBasePlugin.BUILD_GROUP

                    dependsOn(build.assembleTask)

                    into(build.buildProjectName)
                    from(build.artifactsDirectory)

                    archiveBaseName = build.buildProjectName

                }
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

    private fun projectDirExcludingBuildDir(project: Project): ConfigurableFileTree {
        val projectDir = project.projectDir
        val projectTree = project.fileTree(projectDir)

        val buildDir = project.layout.buildDirectory.get().asFile
        if (buildDir.startsWith(projectDir)) {
            projectTree.exclude(buildDir.relativeTo(projectDir).path)
        }

        return projectTree
    }
}