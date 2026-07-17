import com.specificlanguages.buildlogic.ApiCompatibilityCheckTask

plugins {
    java
    id("com.gradle.plugin-publish")
    id("org.gradle.kotlin.kotlin-dsl")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

gradlePlugin {
    website.set("https://specificlanguages.com")
    vcsUrl.set("https://github.com/specificlanguages/mps-gradle-plugin")
}

group = "com.specificlanguages"

repositories {
    mavenCentral()
}

// Fails if the version bump in gradle.properties is smaller than the change to the module's public API
// requires (a removed or changed declaration needs a major bump, a newly added one a minor bump). The public
// API is taken from the binary-compatibility-validator `.api` dump, compared against its last released version.
tasks.register<ApiCompatibilityCheckTask>("checkApiCompatibility") {
    moduleName.set(project.name)
    targetVersion.set(project.version.toString().removeSuffix("-SNAPSHOT"))
    currentApiFile.from(layout.projectDirectory.file("api/${project.name}.api"))
    apiFilePath.set("subprojects/${project.name}/api/${project.name}.api")
    repositoryRoot.set(rootProject.layout.projectDirectory)
}
