package com.specificlanguages.jbrtoolchain

import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec
import java.io.File
import javax.inject.Inject

internal abstract class ToolchainSpecFactory {
    @get:Inject
    abstract val propertyFactory: PropertyFactory

    val method813 = try {
        SpecificInstallationToolchainSpec::class.java.getMethod("fromJavaHome",
            PropertyFactory::class.java, File::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }

    @get:Inject
    abstract val objectFactory: ObjectFactory

    val method812 = try {
        SpecificInstallationToolchainSpec::class.java.getMethod("fromJavaHome",
            ObjectFactory::class.java, File::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }

    fun fromJavaHome(javaHome: File): SpecificInstallationToolchainSpec =
        if (method813 != null) {
            method813.invoke(null, propertyFactory, javaHome) as SpecificInstallationToolchainSpec
        } else if (method812 != null) {
            method812.invoke(null, objectFactory, javaHome) as SpecificInstallationToolchainSpec
        } else  {
            throw IllegalStateException("Unsupported Gradle version, cannot create a SpecificInstallationToolchainSpec")
        }
}
