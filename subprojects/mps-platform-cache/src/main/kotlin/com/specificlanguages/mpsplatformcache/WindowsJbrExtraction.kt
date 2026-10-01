package com.specificlanguages.mpsplatformcache

import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.Untar
import org.apache.tools.ant.util.FileNameMapper
import java.nio.file.Path

internal abstract class WindowsJbrExtraction : JbrExtraction() {
    override fun extractArchive(archive: Path, destination: Path) {
        // Ant is bundled with Gradle and does not require Gradle services inside the ValueSource.
        Untar().apply {
            project = Project().apply { init() }
            setSrc(archive.toFile())
            setDest(destination.toFile())
            setCompression(Untar.UntarCompressionMethod().apply { value = "gzip" })
            add(object : FileNameMapper {
                override fun setFrom(from: String?) = Unit
                override fun setTo(to: String?) = Unit
                override fun mapFileName(sourceFileName: String): Array<String> =
                    arrayOf(sourceFileName.substringAfter('/', ""))
            })
            execute()
        }
    }
}
