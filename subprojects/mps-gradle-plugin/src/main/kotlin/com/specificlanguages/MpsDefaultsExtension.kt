package com.specificlanguages

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

/**
 * MPS defaults, registered with the project as an extension named `mps`.
 */
interface MpsDefaultsExtension {
    /**
     * The home directory of MPS. By default (convention) will be set to the directory where the resolved artifact of
     * `mps` configuration is unpacked.
     */
    val mpsHome: DirectoryProperty

    /**
     * The build script to run. Defaults to `build.xml` in the project directory.
     */
    val buildScript: RegularFileProperty

    /**
     * The directory where dependencies, will be unpacked. Defaults to `$buildDir/dependencies`
     */
    val dependenciesDirectory: DirectoryProperty
}