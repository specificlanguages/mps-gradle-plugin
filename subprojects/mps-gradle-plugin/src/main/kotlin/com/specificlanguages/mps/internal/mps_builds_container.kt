package com.specificlanguages.mps.internal

import com.specificlanguages.mps.MainBuild
import com.specificlanguages.mps.MpsBuild
import com.specificlanguages.mps.RunAnt
import com.specificlanguages.mps.TestBuild
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskContainer

internal fun createMpsBuildsContainer(
    objects: ObjectFactory,
    tasks: TaskContainer,
    generateBuildScriptsTask: Any
): PolymorphicDomainObjectContainer<MpsBuild> {
    val mpsBuilds = objects.polymorphicDomainObjectContainer(MpsBuild::class.java)

    mpsBuilds.registerFactory(MainBuild::class.java, MainBuildFactory(objects, tasks, generateBuildScriptsTask))
    mpsBuilds.registerFactory(TestBuild::class.java, TestBuildFactory(objects, tasks, generateBuildScriptsTask))

    return mpsBuilds
}

private class MainBuildFactory(
    val objects: ObjectFactory,
    val tasks: TaskContainer,
    val generateBuildScriptsTask: Any
) : NamedDomainObjectFactory<MainBuild> {
    override fun create(name: String): MainBuild {
        val generateTask = tasks.register("generate${capitalize(name)}", RunAnt::class.java)
        val assembleTask = tasks.register("assemble${capitalize(name)}", RunAnt::class.java)

        val build = objects.newInstance(MainBuild::class.java, name, generateTask, assembleTask)

        configureGenerateTask(build, generateBuildScriptsTask)
        configureAssembleTask(build)

        return build
    }

}

private class TestBuildFactory(
    val objects: ObjectFactory,
    val tasks: TaskContainer,
    val generateBuildScriptsTask: Any
) : NamedDomainObjectFactory<TestBuild> {
    override fun create(name: String): TestBuild {
        val generateTask = tasks.register("generate${capitalize(name)}", RunAnt::class.java)
        val assembleAndCheckTask = tasks.register("check${capitalize(name)}", RunAnt::class.java)

        val build = objects.newInstance(TestBuild::class.java, name, generateTask, assembleAndCheckTask)

        configureGenerateTask(build, generateBuildScriptsTask)
        configureAssembleAndCheckTask(build)

        return build
    }
}

private fun configureGenerateTask(build: MpsBuild, generateBuildScriptsTask: Any) {
    build.generateTask.configure {
        group = "build"
        description = "Runs 'generate' target of the '${build.name}' build."
        dependsOn(generateBuildScriptsTask)

        buildFile.set(build.buildFile)
        targets.set(listOf("generate"))

        dependsOn(build.dependencies.map { it.map(MainBuild::assembleTask) })
    }
}

private fun configureAssembleTask(build: MainBuild) {
    build.assembleTask.configure {
        group = "build"
        description = "Runs 'assemble' target of the '${build.name}' build."
        dependsOn(build.generateTask)

        buildFile.set(build.buildFile)
        targets.set(listOf("assemble"))

        pathProperties.put("build.layout", build.buildArtifactsDirectory.asFile)
    }
}

private fun configureAssembleAndCheckTask(build: TestBuild) {
    build.assembleAndCheckTask.configure {
        group = "build"
        description = "Runs 'check' target of the '${build.name}' build."
        dependsOn(build.generateTask)

        buildFile.set(build.buildFile)
        targets.set(listOf("check"))

        pathProperties.put("build.layout", build.buildArtifactsDirectory.asFile)
    }
}
