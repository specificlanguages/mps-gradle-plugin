package com.specificlanguages.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Fails a publication that would not resolve for consumers. A project dependency is published into the POM
 * with whatever version the dependency project has at build time, so the module must not be published while a
 * dependency is at a snapshot version or at a release version that has not actually been published yet.
 *
 * Checks that the module's own version is not a snapshot, and that every internal dependency is available at
 * the exact referenced version in the repository consumers resolve from.
 */
abstract class CheckPublishedDependenciesTask : DefaultTask() {

    @get:Input
    abstract val moduleVersion: Property<String>

    /** Coordinates (`group:name:version`) of the module's project dependencies. */
    @get:Input
    abstract val dependencyCoordinates: ListProperty<String>

    /** Base URL of the Maven repository that consumers resolve the dependencies from. */
    @get:Input
    abstract val repositoryUrl: Property<String>

    @TaskAction
    fun check() {
        val version = moduleVersion.get()
        if (version.endsWith("-SNAPSHOT")) {
            throw GradleException(
                "Cannot publish snapshot version $version; run prepareRelease or set a release version first.")
        }

        val problems = mutableListOf<String>()
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        for (coordinates in dependencyCoordinates.get()) {
            val (group, name, depVersion) = coordinates.split(":", limit = 3)
            if (depVersion.endsWith("-SNAPSHOT")) {
                problems.add("dependency '$name' is at snapshot version $depVersion; release it first.")
                continue
            }
            val url = "${repositoryUrl.get().trimEnd('/')}/${group.replace('.', '/')}/$name/$depVersion/" +
                "$name-$depVersion.pom"
            val request = HttpRequest.newBuilder(URI(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
            val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
            when (status) {
                200 -> {}
                404 -> problems.add(
                    "dependency '$name' version $depVersion is not published at ${repositoryUrl.get()}; " +
                        "publish it first (tag '$name-$depVersion').")
                else -> problems.add("could not verify dependency '$name' version $depVersion: HTTP $status for $url")
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Cannot publish version $version:\n" + problems.joinToString("\n") { "  - $it" })
        }
    }
}
