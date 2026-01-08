plugins {
    `plugin-conventions`
}

description = "Cache of MPS and JBR, working around the deficiencies in Gradle artifact transforms"

dependencies {
    gradleApi()

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.hamcrest)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("mpsPlatformCache") {
            id = "com.specificlanguages.mps-platform-cache"
            implementationClass = "com.specificlanguages.mpsplatformcache.MpsPlatformCachePlugin"
            displayName = "MPS Platform Cache"
            description = "Cache MPS and JBR distributions across multiple independent builds"
            tags.set(listOf("jetbrainsMps"))
        }
    }
}
