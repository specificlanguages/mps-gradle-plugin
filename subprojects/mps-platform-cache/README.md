# Overview

A Gradle plugin to extract MPS and JBR distributions into a directory where they can potentially be shared across
multiple independent Gradle builds.

# Usage

The plugin is primarily intended to be used by other plugins, not directly by end users.

The plugin registers a project-level extension, `mpsPlatformCache`, for configuration and to trigger the extraction.

## Configuration

The extension has a single property, `cacheRoot`, to specify the location of the cache. By default, the cache is placed
under the root project's build directory. Thus, the downloaded MPS and JBR distributions are not shared with other
projects by default, but on the other hand, the plugin will not pollute unsuspecting users' directories.

To enable caching between all Gradle projects, override the cache root by setting the Gradle property
`com.specificlanguages.mps-platform-cache.cacheRoot` to a fixed directory. The `~` prefix in the property value is
replaced with the path to the user's home directory.

Example:

```properties
# In $HOME/.gradle/gradle.properties:

# ~ will be replaced with the path to the home directory
com.specificlanguages.mps-platform-cache.cacheRoot=~/.mps-platform-cache
```

## Extraction

The `mpsPlatformCache` extension provides two methods, `getJbrRoot()` and `getMpsRoot()`. Each method accepts a Gradle
`Provider<Configuration>` and returns a `Provider<File>` that when evaluated triggers dependency resolution and
extraction and returns the root directory of the extracted archive. The configuration is expected to contain a single
dependency and resolve to a single artifact. The difference between MPS and JBR extraction is that JBR extraction
is performed using native tools on Unix-like systems (Linux and macOS) in order to preserve symlinks and file
permissions, whereas MPS extraction is done using Java/Gradle means.

Extraction is atomic and parallel-safe.

## Cache cleanup is not performed

There is no mechanism to remove unused distributions from the platform cache, this is left to the user.

## Folder names

`mps-platform-cache` uses shorter folder names for the common MPS and JBR artifact coordinates and more detailed
names for non-standard coordinates. For example, `com.jetbrains:mps:2025.1` would be extracted under `mps/2025.1`
whereas `my.company:custom-rcp:1.0` would be extracted under `mps-custom/my.company/custom-rcp/1.0`.

Having said that, the folder naming policy is considered an implementation detail and may change.
