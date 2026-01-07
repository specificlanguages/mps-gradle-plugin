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

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.hamcrest:hamcrest:3.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}
