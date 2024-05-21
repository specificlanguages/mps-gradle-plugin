import java.io.FileFilter

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "mps-gradle-plugin"

file("subprojects").listFiles(FileFilter { it.isDirectory })!!.forEach {
    include(":${it.name}")
    project(":${it.name}").projectDir = it
}
