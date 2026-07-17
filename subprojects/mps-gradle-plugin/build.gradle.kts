plugins {
    `plugin-conventions`
}

repositories {
    maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
}

// Optional dependencies are compileOnly but the tests exercise the integrations they enable.
configurations.testImplementation.configure { extendsFrom(configurations.compileOnly.get()) }

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":mps-platform-cache"))
    implementation(project(":jbr-toolchain"))
    implementation(libs.mps.launcher)

    // mbeddr plugin is an optional dependency, used only when the user opts in by applying it.
    compileOnly(libs.mbeddr.plugin)

    testImplementation(libs.hamcrest)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    plugins {
        register("mps") {
            id = "com.specificlanguages.mps"
            implementationClass = "com.specificlanguages.mps.MpsPlugin"
            displayName = "MPS Build Plugin"
            description = "Build JetBrains MPS projects using a simple declarative configuration model"
            tags = listOf("jetbrainsMps")
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty(
        "test.mps-platform-cache.cacheRoot",
        rootProject.layout.buildDirectory.dir("mps-platform-cache").get().asFile.absolutePath
    )
}
