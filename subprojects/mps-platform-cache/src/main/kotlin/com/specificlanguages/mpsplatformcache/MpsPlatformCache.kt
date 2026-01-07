package com.specificlanguages.mpsplatformcache

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.file.*
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.support.unzipTo
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.function.BiConsumer
import javax.inject.Inject

abstract class MpsPlatformCache @Inject constructor(
    private val gradle: Gradle,
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
    private val layout: ProjectLayout,
    providers: ProviderFactory,
    objects: ObjectFactory,
) {
    val cacheRoot: DirectoryProperty = objects.directoryProperty().convention(
        providers.gradleProperty("com.specificlanguages.mps-platform-cache.cacheRoot").map(::expandTilde)
            .orElse(gradle.rootProject.layout.buildDirectory.dir("mps-platform-cache")))

    fun getMpsRoot(configuration: Provider<out Configuration>): Provider<File> = configuration.map(::getMpsRoot)

    private fun expandTilde(path: String): Directory? {
        val expandedPath =
            when {
                path == "~" -> System.getProperty("user.home")
                path.startsWith("~/") || path.startsWith("~\\") -> System.getProperty("user.home") + path.substring(1)
                else -> path
            }
        return layout.projectDirectory.dir(expandedPath)
    }

    private fun getMpsRoot(configuration: Configuration): File {
        val module = getSingleModuleIdentifier(configuration)

        val subPath = getMpsFolderPath(module)
        val fullPath = cacheRoot.get().asFile.resolve(subPath)

        extractRobustly(fullPath, configuration.files.single(), ::unzipTo)

        return fullPath
    }

    fun getJbrRoot(configuration: Provider<out Configuration>): Provider<File> = configuration.map(::getJbrRoot)

    private fun getJbrRoot(configuration: Configuration): File {
        val module = getSingleModuleIdentifier(configuration)
        val classifier = configuration.resolvedConfiguration.resolvedArtifacts.single().classifier

        val subPath = getJbrFolderPath(module, classifier)
        val fullPath = cacheRoot.get().asFile.resolve(subPath)

        extractRobustly(fullPath, configuration.files.single(), ::untgzNativelyTo)

        return fullPath
    }

    private fun getMpsFolderPath(id: ModuleComponentIdentifier): String {
        val commonPath = when {
            id.group == "com.jetbrains" && id.module == "mps" -> "mps"
            id.group == "com.jetbrains.mps" && id.module == "mps-prerelease" -> "mps-prerelease"
            else -> "mps-custom" + File.separator + id.group + File.separator + id.module
        }

        return commonPath + File.separator + id.version
    }

    private fun getJbrFolderPath(id: ModuleComponentIdentifier, classifier: String?): String {
        val commonPath = when {
            id.group == "com.jetbrains.jdk" && id.module == "jbr_jcef" -> "jbr_jcef"
            id.group == "com.jetbrains.jdk" && id.module == "jbr" -> "jbr"
            else -> "jbr-custom" + File.separator + id.group + File.separator + id.module
        }

        val classifierSuffix = if (classifier.isNullOrEmpty()) "" else "-$classifier"

        return commonPath + File.separator + id.version + classifierSuffix
    }

    /**
     * Ensures that [distributionDir] contains an extracted distribution. Uses file locking to prevent concurrent
     * extraction from multiple builds. Cleans up partial extractions if the previous attempt failed.
     */
    private fun extractRobustly(distributionDir: File, inputFile: File, extract: BiConsumer<File, File>) {
        val completionFile = getCompletionFileForDistributionDir(distributionDir)
        // Check if extraction is already complete
        if (completionFile.exists()) {
            return
        }

        val lockFile = getLockFileForDistributionDir(distributionDir)

        // Use a file lock to coordinate between multiple builds
        Files.createDirectories(lockFile.parentFile.toPath())
        FileChannel.open(
            lockFile.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        ).use { channel ->
            channel.lock().use locked@{ _ ->
                // Check if extraction was completed while we were waiting for the lock
                if (completionFile.exists()) {
                    // Still delete the lock file at the end
                    return@locked
                }

                // Clean up any partial extraction from a previous failed attempt
                if (distributionDir.exists()) {
                    fileSystemOperations.delete {
                        delete(distributionDir)
                    }
                }

                // Create the target directory and extract
                Files.createDirectories(distributionDir.toPath())
                extract.accept(distributionDir, inputFile)

                // Mark extraction as complete
                completionFile.createNewFile()
            }
        }

        // Try to delete the lock file - it is no longer necessary once the completion file has been created.
        lockFile.delete()
    }

    private fun untgzNativelyTo(outputDir: File, inputFile: File, componentsToStrip: Int = 1) {
        if (Os.isFamily(Os.FAMILY_UNIX)) {
            // Use Unix utilities to properly deal with symlinks
            execOperations.exec {
                commandLine("tar", "--strip-components=$componentsToStrip", "-xzf", inputFile.absolutePath)
                workingDir = outputDir
            }
        } else {
            // On Windows we don't worry about symlinks
            // Using copy rather than sync because we assume we will get a fresh, empty directory from Gradle.
            fileSystemOperations.copy {
                from(archiveOperations.tarTree(inputFile))
                into(outputDir)
                includeEmptyDirs = false

                if (componentsToStrip > 0) {
                    eachFile {
                        // Strip the components
                        relativePath = RelativePath(
                            relativeSourcePath.isFile,
                            *relativeSourcePath.segments.drop(componentsToStrip).toTypedArray()
                        )
                    }
                }
                includeEmptyDirs = false
            }
        }
    }

    companion object {
        internal fun getLockFileForDistributionDir(distributionDir: File) =
            File(distributionDir.parentFile, "${distributionDir.name}.lock")

        internal fun getCompletionFileForDistributionDir(distributionDir: File) = File(distributionDir, ".complete")

        /**
         * Resolve [configuration] to a single [ModuleComponentIdentifier]
         */
        private fun getSingleModuleIdentifier(configuration: Configuration): ModuleComponentIdentifier {
            val resolutionResult = configuration.incoming.resolutionResult
            val dependencies = resolutionResult.root.dependencies
            val dependencyResult = dependencies.singleOrNull()
                ?: throw IllegalStateException("Expected a single dependency for configuration '${configuration.name}', found ${dependencies.size} dependencies")

            if (dependencyResult is UnresolvedDependencyResult) {
                throw IllegalStateException(
                    "Could not resolve configuration '${configuration.name}'",
                    dependencyResult.failure
                )
            }

            if (dependencyResult !is ResolvedDependencyResult) {
                // Shouldn't happen
                throw IllegalStateException("Could not resolve configuration '${configuration.name}': $dependencyResult")
            }

            val selectedId = dependencyResult.selected.id
            if (selectedId !is ModuleComponentIdentifier) {
                throw IllegalStateException("Configuration '${configuration.name}' must contain a module dependency. The dependency was resolved to $selectedId instead.")
            }
            return selectedId
        }
    }
}
