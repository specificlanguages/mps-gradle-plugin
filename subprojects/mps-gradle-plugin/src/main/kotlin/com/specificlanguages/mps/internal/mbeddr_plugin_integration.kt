package com.specificlanguages.mps.internal

import com.specificlanguages.mps.MpsDefaultsExtension
import de.itemis.mps.gradle.tasks.MpsCheck
import de.itemis.mps.gradle.tasks.MpsExecute
import de.itemis.mps.gradle.tasks.MpsGenerate
import de.itemis.mps.gradle.tasks.MpsMigrate
import de.itemis.mps.gradle.tasks.Remigrate
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import java.io.File

private const val MBEDDR_PLUGIN_ID = "de.itemis.mps.gradle.common"

// Configures the optional integration with the mbeddr plugin (`de.itemis.mps.gradle.common`, coords
// `de.itemis.mps:mps-gradle-plugin`). When the user applies it alongside this plugin, its tasks are given the same
// defaults as the tasks this plugin owns:
//   - mpsHome and javaLauncher conventions from mpsDefaults,
//   - the MPS plugins directory as a plugin root,
//   - mpsDefaults.pathVariables as macros.
// Gated by withPlugin so users who do not apply the mbeddr plugin never load its task types.
internal fun configureMbeddrIntegration(
    project: Project,
    mpsDefaults: MpsDefaultsExtension,
) {
    project.pluginManager.withPlugin(MBEDDR_PLUGIN_ID) {
        applyMbeddrTaskDefaults(project, mpsDefaults)
    }
}

private fun fileToDirectory(layout: ProjectLayout, file: File) = layout.projectDirectory.dir(file.path)

private fun filesToDirectories(layout: ProjectLayout, map: Map<String, File>): Map<String, Directory> =
    map.mapValues { (_, file) -> fileToDirectory(layout, file) }

private fun filesAsDirectories(layout: ProjectLayout, mapProvider: Provider<Map<String, File>>) =
    mapProvider.map { filesToDirectories(layout, it) }

private fun applyMbeddrTaskDefaults(project: Project, mpsDefaults: MpsDefaultsExtension) {
    // JavaExec-based tasks inherit javaLauncher from JavaExec; MpsMigrate declares its own.
    val pathVariablesAsDirectories = filesAsDirectories(project.layout, mpsDefaults.pathVariables)
    project.tasks.withType(MpsCheck::class.java).configureEach {
        mpsHome.convention(mpsDefaults.mpsHome)
        javaLauncher.convention(mpsDefaults.javaLauncher)
        pluginRoots.add(mpsHome.dir("plugins"))
        folderMacros.putAll(pathVariablesAsDirectories)
    }
    project.tasks.withType(MpsExecute::class.java).configureEach {
        mpsHome.convention(mpsDefaults.mpsHome)
        javaLauncher.convention(mpsDefaults.javaLauncher)
        pluginRoots.add(mpsHome.dir("plugins"))
        // MpsExecute takes macro values as strings rather than as directories.
        macros.putAll(pathVariablesAsDirectories.map { m -> m.mapValues { it.value.asFile.path } })
    }
    project.tasks.withType(MpsGenerate::class.java).configureEach {
        mpsHome.convention(mpsDefaults.mpsHome)
        javaLauncher.convention(mpsDefaults.javaLauncher)
        pluginRoots.from(mpsHome.dir("plugins"))
        folderMacros.putAll(pathVariablesAsDirectories)
    }
    project.tasks.withType(MpsMigrate::class.java).configureEach {
        mpsHome.convention(mpsDefaults.mpsHome)
        javaLauncher.convention(mpsDefaults.javaLauncher)
        pluginRoots.from(mpsHome.dir("plugins"))
        folderMacros.putAll(pathVariablesAsDirectories)
    }
    project.tasks.withType(Remigrate::class.java).configureEach {
        mpsHome.convention(mpsDefaults.mpsHome)
        javaLauncher.convention(mpsDefaults.javaLauncher)
        pluginRoots.from(mpsHome.dir("plugins"))
        folderMacros.putAll(pathVariablesAsDirectories)
    }
}
