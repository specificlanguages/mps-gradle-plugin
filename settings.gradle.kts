plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "mps-gradle-plugin"

file("subprojects").listFiles { it.isDirectory && it.resolve("build.gradle.kts").isFile }?.forEach {
    include(":${it.name}")
    project(":${it.name}").projectDir = it
}
