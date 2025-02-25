# Overview

A Gradle plugin to download a specific version of the JetBrains Runtime (JBR) and expose it as
a [JVM toolchain](https://docs.gradle.org/current/userguide/toolchains.html).

# Usage

```kotlin
plugins {
    id("com.specificlanguages.jbr-toolchain") version "1.0.2"
}

dependencies {
    // The plugin creates 'jbr' configuration and sets up dependency substitution to download the correct artifact for
    // the current platform based on the specified dependency
    jbr("com.jetbrains.jdk:jbr_jcef:21.0.5-b631.8")
}

repositories {
    // The plugin assumes that the JBR is packaged according to the conventions in the itemis repository.
    maven("https://artifacts.itemis.cloud/repository/maven-mps")
}

val runJava by tasks.registering(JavaExec::class) {
    // jbrToolchain extension offers 'javaLauncher' property that can be used here directly  
    javaLauncher = jbrToolchain.javaLauncher

    // ...
}

tasks.withType<JavaCompile>().configureEach {
    // jbrToolchain also has a 'spec' property that can be used with the 'javaToolchains' extension to obtain other
    // supported tools (currently the compiler and javadoc tool).
    javaCompiler = javaToolchains.compilerFor(jbrToolchain.spec)
}
```

# Important notes

The plugin was built for the MPS/mbeddr ecosystem and assumes that the JBR artifacts are published according to the
conventions of [mbeddr/build.publish.jdk](https://github.com/mbeddr/build.publish.jdk). JBRs for new MPS releases are
regularly published by itemis to its [Nexus repository](https://artifacts.itemis.cloud/repository/maven-mps).

# Why jbr-toolchain

When building MPS projects it is often desirable to use the version of the JBR that exactly matches the one used by a
particular MPS version and the current platform. While
Gradle [supports downloading a custom JVM](https://docs.gradle.org/current/userguide/toolchain_plugins.html), it only
allows specifying the language version, e.g. 17 or 21, which is not specific enough in this case.

This plugin allows specifying the exact version of the JBR to download and use for executing Java, including running
MPS build scripts. It is implemented according to the current best practices of Gradle and thus supports caching,
dependency locking and sharing of the JBR between multiple builds, even those that are independent.

# Improvements over download-jbr

So far, the problem of downloading a specific JBR was primarily adressed by
the [download-jbr](https://github.com/mbeddr/mps-gradle-plugin/blob/v1.x/docs/plugins/download-jbr.md) plugin, part
of [mbeddr/mps-gradle-plugin](https://github.com/mbeddr/mps-gradle-plugin/tree/v1.x). That plugin was written a long
time ago and while it is very simple and works well enough for simple cases, it has certain shortcomings that become
apparent in large projects.

First, download-jbr only has limited support for laziness and configuration avoidance.

Second, to share a JBR downloaded by download-jbr among multiple projects, it is necessary to use cross-project task
dependencies (project Foo must depend on the `downloadJbr` task of project Bar). Such dependencies
are [discouraged](https://docs.gradle.org/current/userguide/declaring_dependencies_between_subprojects.html#sec:depending_on_output_of_another_project)
by Gradle.

Third, even if a cross-project dependency is used, the downloaded JBR is not available for use by any other builds, each
build has to download an independent copy of the JBR.

Fourth, the JBR dependency is created on the fly and resolved in a detached configuration. As a result, it is not
visible to dependency management tooling such as dependency locking and automated upgrades.

The jbr-toolchain plugin solves these problems at a cost of slightly higher implementation complexity.

# How it works

The plugin creates a configuration called `jbr` and sets
up [dependency substitution](https://docs.gradle.org/current/userguide/resolution_rules.html#sec:dependency-substitution-rules)
to add a classifier corresponding to the current operating system and architecture (e.g. `osx-aarch64`).

In addition, an [artifact transform](https://docs.gradle.org/current/userguide/artifact_transforms.html) is used to
extract the JBR. Gradle shares the result of the transform among all projects
(keyed by the artifact coordinates and the hash of the transform implementation class and its classpath), just like it
shares other downloaded Java dependencies.

The use of the artifact transform is hidden behind the `jbrToolchain` extension facade. This extension provides a
`JavaLauncher` for direct use in `JavaExec` and similar tasks, and a `ToolchainSpec` object to access any other tools.
