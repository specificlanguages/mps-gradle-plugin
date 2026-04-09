plugins {
    `plugin-conventions`
}

repositories {
    maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":mps-platform-cache"))
    implementation(project(":jbr-toolchain"))
    implementation(libs.mps.launcher)

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
            tags.set(listOf("jetbrainsMps"))
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
