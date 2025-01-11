package com.specificlanguages.jbrtoolchain

import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import javax.inject.Inject

@Suppress("Unused", "CanBeParameter")
abstract class JbrToolchainExtension(val spec: Provider<JavaToolchainSpec>) {
    @get:Inject
    abstract val javaToolchainService: JavaToolchainService

    val javaLauncher = spec.flatMap(javaToolchainService::launcherFor)
}
