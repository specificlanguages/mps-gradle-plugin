plugins {
    `plugin-conventions`
}

description = "Cache of MPS and JBR, working around the deficiencies in Gradle artifact transforms"

dependencies {
    gradleApi()

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.hamcrest:hamcrest:3.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
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
