package com.specificlanguages.mpsplatformcache

import org.gradle.process.ExecOperations
import java.nio.file.Path
import javax.inject.Inject

internal abstract class NativeJbrExtraction : JbrExtraction() {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun extractArchive(archive: Path, destination: Path) {
        // Native tar preserves the runtime's symbolic links.
        execOperations.exec {
            commandLine("tar", "--strip-components=1", "-xzf", archive.toAbsolutePath().toString())
            workingDir = destination.toFile()
        }
    }
}
