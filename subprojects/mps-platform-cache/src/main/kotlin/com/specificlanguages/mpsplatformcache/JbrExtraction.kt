package com.specificlanguages.mpsplatformcache

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.nio.file.Path

internal abstract class JbrExtraction : ValueSource<File, JbrExtraction.Parameters> {
    interface Parameters : ValueSourceParameters {
        val archive: RegularFileProperty
        val directory: DirectoryProperty
    }

    override fun obtain(): File {
        val directory = parameters.directory.get().asFile
        try {
            DistributionExtraction().ensureExtracted(
                parameters.archive.get().asFile.toPath(), directory.toPath(), ::extractArchive
            )
        } catch (e: ExtractionLockTimeoutException) {
            throw GradleException(e.message, e)
        }
        return directory
    }

    protected abstract fun extractArchive(archive: Path, destination: Path)
}
