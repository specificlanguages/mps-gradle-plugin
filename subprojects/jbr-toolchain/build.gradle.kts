plugins {
    `plugin-conventions`
}

gradlePlugin {
    plugins {
        register("jbrToolchain") {
            id = "com.specificlanguages.jbr-toolchain"
            implementationClass = "com.specificlanguages.jbrtoolchain.JbrToolchainPlugin"
            displayName = "JBR Toolchain"
            description = "Download a specific version of the JetBrains Runtime (JBR) and expose it as a JVM toolchain"
            tags.set(listOf("jetbrainsRuntime", "jbr"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":mps-platform-cache"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.hamcrest)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
