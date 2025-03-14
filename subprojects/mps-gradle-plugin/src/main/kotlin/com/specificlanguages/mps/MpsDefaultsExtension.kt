package com.specificlanguages.mps

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import java.io.File

/**
 * MPS defaults, registered with the project as an extension named `mps`.
 */
interface MpsDefaultsExtension {
    /**
     * The home directory of MPS. Defaults to the single directory in the `mpsHome` configuration.
     */
    val mpsHome: DirectoryProperty

    /**
     * The directory where MPS libraries will be unpacked. Defaults to `$buildDir/dependencies`.
     */
    val mpsLibrariesDirectory: DirectoryProperty

    /**
     * The Java executable that [RunAnt] will use by default to run Ant. By default, the launcher provided by
     * the `jbr-toolchain` plugin will be used.
     */
    val javaLauncher: Property<JavaLauncher>

    /**
     * Path variables/folder macros, added to both [RunAnt.pathProperties] and [GenerateBuildScripts.pathVariables].
     */
    val pathVariables: MapProperty<String, File>

    /**
     * Ant classpath to use for [RunAnt]. The default is `[mpsHome]/lib/ant/lib/\*.jar`, excluding `ant-mps.jar`.
     */
     val antClasspath: ConfigurableFileCollection
}