import com.specificlanguages.buildlogic.ApiCompatibilityCheckTask
import com.specificlanguages.buildlogic.CheckPublishedDependenciesTask
import com.specificlanguages.buildlogic.PrepareReleaseTask
import com.specificlanguages.buildlogic.TagReleaseTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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

tasks.withType<Test>().configureEach {
    testLogging {
        events(TestLogEvent.FAILED)
        // The tests run nested Gradle builds through TestKit, which report their failures in the exception
        // message. Without the full format, the reason a nested build failed is lost.
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}

gradlePlugin {
    website = "https://specificlanguages.com"
    vcsUrl = "https://github.com/specificlanguages/mps-gradle-plugin"
}

group = "com.specificlanguages"

repositories {
    mavenCentral()
}

val moduleVersionProvider = provider { project.version.toString() }

// Fails if the version bump in gradle.properties is smaller than the change to the module's public API
// requires (a removed or changed declaration needs a major bump, a newly added one a minor bump). The public
// API is taken from the binary-compatibility-validator `.api` dump, compared against its last released version.
tasks.register<ApiCompatibilityCheckTask>("checkApiCompatibility") {
    moduleName = project.name
    targetVersion = moduleVersionProvider.map { it.removeSuffix("-SNAPSHOT") }
    currentApiFile.from(layout.projectDirectory.file("api/${project.name}.api"))
    apiFilePath = "subprojects/${project.name}/api/${project.name}.api"
    repositoryRoot = rootProject.layout.projectDirectory
}

// The version of a project dependency lands in the published POM as-is, so publishing must not proceed while
// a dependency is at a snapshot version or at a release version that has not been published yet.
val projectDependencyCoordinates = provider {
    listOf("api", "implementation")
        .mapNotNull { configurations.findByName(it) }
        .flatMap { it.dependencies.withType<ProjectDependency>() }
        .map { "${it.group}:${it.name}:${it.version}" }
}

val checkPublishedDependencies = tasks.register<CheckPublishedDependenciesTask>("checkPublishedDependencies") {
    group = "verification"
    description = "Checks that the published POM will not reference snapshot or unpublished dependencies."
    moduleVersion = moduleVersionProvider
    dependencyCoordinates = projectDependencyCoordinates
    repositoryUrl = "https://plugins.gradle.org/m2"
}

tasks.named("publishPlugins") {
    dependsOn(checkPublishedDependencies)
}

tasks.register<PrepareReleaseTask>("prepareRelease") {
    group = "publishing"
    description = "Sets the release version, patches the changelog, and creates the release commit."
    moduleName = project.name
    currentVersion = moduleVersionProvider
    changelogFile = layout.projectDirectory.file("CHANGELOG.md")
    propertiesFile = layout.projectDirectory.file("gradle.properties")
    repositoryRoot = rootProject.layout.projectDirectory
    dependsOn(":checkReleaseVersions")
}

tasks.register<TagReleaseTask>("tagRelease") {
    group = "publishing"
    description = "Creates the release tag on master after the release commit has been merged."
    moduleName = project.name
    currentVersion = moduleVersionProvider
    changelogFile = layout.projectDirectory.file("CHANGELOG.md")
    repositoryRoot = rootProject.layout.projectDirectory
    dependsOn(":checkReleaseVersions")
    // Pushing the tag triggers publishing, so an unpublishable state must surface before the tag exists,
    // not in the publish workflow after the tag has already been pushed.
    dependsOn(checkPublishedDependencies)
}
