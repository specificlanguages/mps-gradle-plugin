package com.specificlanguages.jbrtoolchain.internal

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ExtractJbrTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputDir = outputs.dir("jbr")

        if (Os.isFamily(Os.FAMILY_UNIX)) {
            // Use Unix utilities to properly deal with symlinks
            execOperations.exec {
                commandLine("tar", "--strip-components=1", "-xzf", inputFile.absolutePath)
                workingDir = outputDir
            }
        } else {
            // On Windows we don't worry about symlinks
            // Using copy rather than sync because we assume we will get a fresh, empty directory from Gradle.
            fileSystemOperations.copy {
                from({ archiveOperations.tarTree(inputFile) })
                into(outputDir)
                includeEmptyDirs = false
                eachFile {
                    // Strip the first directory
                    relativePath = RelativePath(relativeSourcePath.isFile, *relativeSourcePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
        }
    }
}
