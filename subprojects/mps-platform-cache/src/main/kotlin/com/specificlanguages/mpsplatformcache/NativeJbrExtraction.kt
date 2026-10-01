package com.specificlanguages.mpsplatformcache

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class NativeJbrExtraction : ValueSource<File, NativeJbrExtraction.Parameters> {
    interface Parameters : ValueSourceParameters {
        val archive: RegularFileProperty
        val directory: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): File {
        val directory = parameters.directory.get().asFile
        try {
            DistributionExtraction().ensureExtracted(
                parameters.archive.get().asFile.toPath(), directory.toPath()
            ) { archive, destination ->
                // Native tar preserves the runtime's symbolic links.
                execOperations.exec {
                    commandLine("tar", "--strip-components=1", "-xzf", archive.toAbsolutePath().toString())
                    workingDir = destination.toFile()
                }
            }
        } catch (e: ExtractionLockTimeoutException) {
            throw GradleException(e.message, e)
        }
        return directory
    }
}
