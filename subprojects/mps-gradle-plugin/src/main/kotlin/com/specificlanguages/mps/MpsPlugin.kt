package com.specificlanguages.mps

import com.specificlanguages.jbrtoolchain.JbrToolchainExtension
import com.specificlanguages.jbrtoolchain.JbrToolchainPlugin
import com.specificlanguages.mps.internal.ConfigurationNames
import com.specificlanguages.mps.internal.configureMbeddrIntegration
import com.specificlanguages.mps.internal.configureLibrariesArtifactsProperties
import com.specificlanguages.mps.internal.configureProjectDependencies
import com.specificlanguages.mps.internal.writeArtifactsProperties
import com.specificlanguages.mps.internal.createBundledDependenciesContainer
import com.specificlanguages.mps.internal.createMpsBuildsContainer
import com.specificlanguages.mps.internal.registerArtifactDirectoriesVariant
import com.specificlanguages.mpsplatformcache.MpsPlatformCache
import com.specificlanguages.mpsplatformcache.MpsPlatformCachePlugin
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.collections.emptyList

/**
 * Finds directories with MPS-generated files under [roots]. Does not follow symbolic links and does not descend into
 * hidden directories or [excludedDirs].
 */
private fun allGeneratedDirs(roots: Set<File>, excludedDirs: Set<File>): Set<File> {
    val dirsToFind = arrayOf("source_gen", "source_gen.caches", "classes_gen", "test_gen", "test_gen.caches")
    val result = mutableSetOf<File>()
    val stack = roots.toMutableList()

    while (stack.isNotEmpty()) {
        val top = stack.removeAt(stack.size - 1)
        val files = top.listFiles() ?: continue

        for (file in files) {
            if (file.name.startsWith(".") || file in excludedDirs) { continue }
            if (Files.isSymbolicLink(file.toPath()) || !file.isDirectory) { continue }

            if (file.name in dirsToFind) {
                result.add(file)
            } else {
                stack.add(file)
            }
        }
    }

    return result
}

private const val CLEAN_MPS_GENERATED_TASK_NAME = "cleanMpsGenerated"

@Suppress("unused", "DEPRECATION")
open class MpsPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory,
    private val toolchains: JavaToolchainService,
    private val fileSystemOperations: FileSystemOperations
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.run {
            pluginManager.apply(BasePlugin::class.java)
            pluginManager.apply(MpsPlatformCachePlugin::class.java)
            pluginManager.apply(JbrToolchainPlugin::class.java)

            val mpsConfiguration = registerMpsConfiguration(configurations)

            val mpsPlatformCache = MpsPlatformCachePlugin.getMpsPlatformCache(project)

            val mpsDefaults = registerMpsDefaultsExtension(extensions, layout, mpsConfiguration, mpsPlatformCache)

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

            val mpsLibrariesArtifactsProperties = layout.buildDirectory.file("mps-libraries-artifacts.properties")

            val resolveMpsLibraries by tasks.registering(Sync::class) {
                // Libraries of other projects (in this build or in included builds) are not extracted. To open the
                // project in MPS, their sources are expected to be added to the MPS project as project libraries. The
                // MPS build scripts use their build artifacts directories instead (see configureProjectDependencies).
                val externalLibraries = mpsLibraries.map { cfg ->
                    cfg.incoming.artifactView { componentFilter { it !is ProjectComponentIdentifier } }.files
                }
                dependsOn(externalLibraries)
                from(externalLibraries.map { files -> files.map(project::zipTree) })

                into(mpsDefaults.mpsLibrariesDirectory)
                group = "build setup"
                description = "Downloads and extracts all external MPS libraries."

                val librariesDirectory = mpsDefaults.mpsLibrariesDirectory
                outputs.file(mpsLibrariesArtifactsProperties)
                doLast {
                    writeArtifactsProperties(librariesDirectory.get().asFile, mpsLibrariesArtifactsProperties.get().asFile)
                }
            }

            val setupTask = tasks.register("setup") {
                dependsOn(resolveMpsLibraries)
                dependsOn(Callable { bundledDependencies.map(BundledDependency::resolveTask) })
                dependsOn(Callable {
                    listOf(apiConfiguration.get(), testImplementationConfiguration.get())
                        .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                        .map { dependency -> if (dependency.path == ":") ":setup" else "${dependency.path}:setup" }
                        .distinct()
                })

                group = "build setup"
                description = "Sets up the project and the projects it depends on so that it can be opened in MPS."
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
            configureProjectDependencies(mpsBuilds, mpsLibraries, providers)
            configureLibrariesArtifactsProperties(
                mpsBuilds,
                files(mpsLibrariesArtifactsProperties).builtBy(resolveMpsLibraries)
            )

            configureMbeddrIntegration(project, mpsDefaults)

            generateBuildScriptsTask.configure {
                dependsOn(setupTask)
                buildSolutionDescriptorsByProject.putAll(provider {
                    mpsBuilds.groupBy({ it.mpsProjectDirectory.get() }, { it.buildSolutionDescriptor.get() })
                })
            }

            // Directories with MPS-generated files are searched for when the task runs, so that the search does not
            // make the files of the project inputs of the configuration cache. Directories of subprojects are skipped,
            // they belong to the subprojects.
            val cleanMpsGeneratedTask = tasks.register(CLEAN_MPS_GENERATED_TASK_NAME) {
                group = LifecycleBasePlugin.BUILD_GROUP
                description = "Deletes directories with MPS-generated files (source_gen, source_gen.caches, " +
                        "classes_gen, test_gen, test_gen.caches) in the MPS projects of all MPS builds."

                val mpsProjectDirectories = provider { mpsBuilds.map { it.mpsProjectDirectory.get().asFile }.toSet() }
                val buildDirectory = layout.buildDirectory.asFile
                val subprojectDirectories = subprojects.map { it.projectDir }.toSet()
                val fileSystemOperations = fileSystemOperations
                doLast {
                    val generatedDirs = allGeneratedDirs(
                        mpsProjectDirectories.get(),
                        subprojectDirectories + buildDirectory.get()
                    )
                    fileSystemOperations.delete { delete(generatedDirs) }
                }
            }

            tasks.named(BasePlugin.CLEAN_TASK_NAME) {
                dependsOn(cleanMpsGeneratedTask)
            }

            val zipTask = registerZipTask(mpsBuilds, this.tasks)

            configureLifecycleTasks(tasks, mpsBuilds, zipTask)

            val apiElementsConfiguration =
                registerApiElementsConfiguration(apiConfiguration, zipTask, configurations, objects)
            registerArtifactDirectoriesVariant(apiElementsConfiguration, mpsBuilds, providers)

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
    ): TaskProvider<Zip> = tasks.register("zip", Zip::class.java) {
        this.group = LifecycleBasePlugin.BUILD_GROUP
        this.description = "Packages the artifacts of all main published MPS builds into a ZIP archive."

        fun addToZipIfPublished(build: MainBuild) {
            val task = this
            val published = build.published

            task.dependsOn(build.published.map { if (it) build.assembleTask else emptyList<Task>() })

            task.into(build.published.map { if (it) build.buildArtifactsDirectory.asFile.map(File::getName) else "" }) {
                from(build.published.map {
                    if (it) build.buildArtifactsDirectory
                    else project.layout.buildDirectory.dir("nonexistent")
                })
                exclude { !published.getOrElse(false) }
            }
        }

        mpsBuilds.withType(MainBuild::class.java).all { addToZipIfPublished(this) }
    }

    private fun configureLifecycleTasks(
        tasks: TaskContainer,
        mpsBuilds: DomainObjectCollection<MpsBuild>,
        zipTask: TaskProvider<out Task>
    ) {
        tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(zipTask)
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
        }

    private fun registerMpsDefaultsExtension(
        extensions: ExtensionContainer,
        layout: ProjectLayout,
        mpsConfiguration: Provider<out Configuration>,
        mpsPlatformCache: MpsPlatformCache
    ): MpsDefaultsExtension =
        extensions.create<MpsDefaultsExtension>("mpsDefaults").apply {
            mpsHome.convention(layout.dir(mpsPlatformCache.getMpsRoot(mpsConfiguration)))
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
            // RunAnt tasks declare no outputs, and the cleanMpsGenerated task does not declare the directories it
            // deletes, so Gradle would not otherwise keep the clean tasks from running at the same time.
            mustRunAfter(BasePlugin.CLEAN_TASK_NAME, CLEAN_MPS_GENERATED_TASK_NAME)

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
