import com.specificlanguages.buildlogic.ApiCompatibilityCheckTask
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
