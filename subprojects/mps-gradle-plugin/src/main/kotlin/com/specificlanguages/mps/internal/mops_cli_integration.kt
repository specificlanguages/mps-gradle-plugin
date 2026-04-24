package com.specificlanguages.mps.internal

import com.specificlanguages.mps.MpsBuild
import de.itemis.mps.gradle.tasks.MpsCheck
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project

private const val MBEDDR_PLUGIN_ID = "de.itemis.mps.gradle.common"
private const val MOPS_CHECK_TASK = "mopsCheck"
private const val MODELS_PROP = "mops.models"
private const val MODULES_PROP = "mops.modules"
private const val PROJECT_LOCATION_PROP = "mops.projectLocation"

internal fun configureMopsCliIntegration(
    project: Project,
    mpsBuilds: PolymorphicDomainObjectContainer<MpsBuild>,
) {
    project.pluginManager.withPlugin(MBEDDR_PLUGIN_ID) {
        registerMopsCheck(project, mpsBuilds)
    }
}

private fun registerMopsCheck(project: Project, mpsBuilds: PolymorphicDomainObjectContainer<MpsBuild>) {
    if (MOPS_CHECK_TASK in project.tasks.names) return

    val mopsCheck = project.tasks.register(MOPS_CHECK_TASK, MpsCheck::class.java)
    mopsCheck.configure {
        group = "verification"
        description = "Runs MPS model checks; -Pmops.{models,modules,projectLocation} are honored by the mops CLI."
        applyMopsProperties(project, this)
    }

    project.afterEvaluate {
        val dirs = mpsBuilds.mapNotNull { it.mpsProjectDirectory.orNull }.distinct()
        dirs.singleOrNull()?.let { single ->
            mopsCheck.configure { projectLocation.convention(single) }
        }
    }
}

private fun applyMopsProperties(project: Project, task: MpsCheck) {
    (project.findProperty(MODELS_PROP) as? String)?.takeIf(String::isNotEmpty)
        ?.let { task.models.set(it.split(',')) }
    (project.findProperty(MODULES_PROP) as? String)?.takeIf(String::isNotEmpty)
        ?.let { task.modules.set(it.split(',')) }
    (project.findProperty(PROJECT_LOCATION_PROP) as? String)?.takeIf(String::isNotEmpty)
        ?.let { task.projectLocation.set(project.layout.projectDirectory.dir(it)) }
}
