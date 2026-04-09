import com.specificlanguages.mps.MainBuild

plugins {
    id("com.specificlanguages.mps")
}

repositories {
    maven("https://artifacts.itemis.cloud/repository/maven-mps/")
    mavenCentral()
}

dependencies {
    mps("com.jetbrains:mps:2024.3.1")
    jbr("com.jetbrains.jdk:jbr_jcef:17.0.6-b469.82")
}

group = "com.specificlanguages.mps-json"
version = "1.0.0-test"

mpsBuilds {
    create<MainBuild>("main") {
        buildSolutionDescriptor = file("solutions/com.specificlanguages.json.build/com.specificlanguages.json.build.msd")
        buildFile = layout.projectDirectory.file("build.xml")
        buildArtifactsDirectory = layout.buildDirectory.dir("artifacts/com.specificlanguages.json")
    }
}
