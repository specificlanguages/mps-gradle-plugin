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
    implementation("de.itemis.mps.build-backends:launcher:2.5.2.120.c791da5")

    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
}
