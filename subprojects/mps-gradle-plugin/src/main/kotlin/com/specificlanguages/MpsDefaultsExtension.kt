package com.specificlanguages

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainSpec

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

    /**
     * Java executable that [RunAntScript] will use by default to run Ant.
     *
     * Prefer [javaLauncher] over [javaExecutable]. Setting [javaExecutable] overrides [javaLauncher].
     */
    val javaExecutable: RegularFileProperty

    /**
     * Java executable that [RunAntScript] will use by default to run Ant.
     *
     * Prefer [javaLauncher] over [javaExecutable]. Setting [javaExecutable] overrides [javaLauncher].
     */
    val javaLauncher: Property<JavaLauncher>
}
