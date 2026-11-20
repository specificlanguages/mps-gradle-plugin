package com.specificlanguages.mps.internal

import com.specificlanguages.mps.MpsBuild
import org.gradle.api.DomainObjectCollection
import org.gradle.api.file.FileCollection
import java.io.File
import java.util.Properties

/**
 * Writes an `artifacts.<name>` property for each top-level directory `<name>` of the extracted MPS libraries in
 * [librariesDirectory].
 *
 * Zips produced by this plugin contain the artifacts directory of each published main build as a top-level directory.
 * Its name usually matches the name of the MPS build project, and MPS-generated Ant scripts read the location of the
 * artifacts of a dependency build project named `X` from the `artifacts.X` property.
 */
internal fun writeArtifactsProperties(librariesDirectory: File, propertiesFile: File) {
    val properties = Properties()
    librariesDirectory.listFiles()
        ?.filter { it.isDirectory }
        ?.forEach { properties["artifacts.${it.name}"] = it.absolutePath }

    propertiesFile.parentFile.mkdirs()
    propertiesFile.outputStream().use { properties.store(it, null) }
}

/**
 * Passes [propertiesFile] (written by [writeArtifactsProperties]) to the Ant tasks of all [mpsBuilds].
 */
internal fun configureLibrariesArtifactsProperties(
    mpsBuilds: DomainObjectCollection<MpsBuild>,
    propertiesFile: FileCollection
) {
    mpsBuilds.configureEach {
        antTasks(this).forEach { task ->
            task.configure { propertyFiles.from(propertiesFile) }
        }
    }
}
