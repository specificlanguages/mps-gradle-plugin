# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 2.0.0-pre4

### Changed

- `package` task renamed to `packageZip` to avoid conflict with the `package` reserved word.

## 2.0.0-pre3

### Added

- `MpsBuild.mpsProjectDirectory` property to support multiple MPS projects in a single Gradle project.
- `MainBuild.published` property to control whether a build's artifacts are included in the published package.
- `RunAnt`: pass additional path properties to Ant by default: `mps.home`, `build.jna.library.path`,
  `build.mps.config.path`, and `build.mps.system.path`. The latter two properties are overridden to improve task
  isolation.

### Changed

- Task properties in MpsBuild classes replaced with task name getters:
- `MpsBuild.buildProjectName` removed, its value will be derived from `MpsBuild.buildArtifactsDirectory` which is now
  required.

  The rationale for this change is that the artifacts directory is more important for the functionality of the plugin.
  Also, users did not quite understand what to provide for 'build project name,' probably because it was unexpected.

## 2.0.0-pre2

This version fixes a few bugs in 2.0.0-pre1 and introduces fine-grained configurations named consistently with Gradle
Java plugins. The existing `generation` configuration is renamed to `api` and a new configuration, `testImplementation`,
is introduced for test-only dependencies.

Even though MPS does not distinguish between 'runtime' and 'compile' dependencies like the Java ecosystem does,
I believe it will be beneficial to make this distinction in this plugin at least, in the hope that it may be used by
third-party tools in the future to guard against implementation dependencies leaking into the library interface.

### Added

- Fine-grained dependencies (`api`, `testImplementation`, etc.).
- `GenerateBuildScripts.environment` for specifying the environment. Defaults to the environment of the current process.
- `RunAnt` will delete its working directory unless told not to.

### Fixed

- `RunAnt` environment now defaults to the environment of the current process.
- The plugin no longer causes all `RunAnt` and `GenerateBuildScript` tasks to be eagerly realized (created).

## Changed

- `generation` configuration renamed to `api`.
- `mpsDefaults.dependenciesDirectory` renamed to `mpsLibrariesDirectory` to be more specific.
- `resolveGenerationDependencies` task renamed to `resolveMpsLibraries` as it resolves all dependencies, not just
  generation-time.

## 2.0.0-pre1

Breaking changes are to be expected until the final 2.0.0 release.

### Added

- Path variables can be specified in `mpsDefaults` and will be passed to both MPS and Ant tasks.
- Support for multiple build scripts, including test build scripts.

### 🚨 Changed

- Automatic build script detection was removed, build scripts now have to be explicitly added to the `mpsBuilds`
  container.
- The plugin now applies the `jbr-toolchain` plugin and uses the runtime specified by the `jbr` configuration to run
  Ant and MPS.
- `stubs` renamed to `bundledDependencies`
- 🚨 `com.specificlanguages.RunAntScript` replaced with `com.specificlanguages.mps.RunAnt` which uses modern Gradle
  features: lazy properties and `JavaLauncher` for specifying the JVM to use.
- 🚨 The `com.specificlanguages.mps` plugin no longer applies the `java-base` plugin.

### 🚨 Removed

- `mpsDefaults.javaExecutable` (previously deprecated in favor of `mpsDefaults.javaLauncher`).

## 1.9.0

### Added

- `mpsDefaults.javaLauncher` to specify the Java executable for MPS in a more modern way and support
  the `com.specificlanguages.jbr-toolchain` plugin.

### Deprecated

- The `javaExecutable` property added in the previous version is deprecated in favor of the new `javaLauncher` property.

## 1.8.0

### Added

- Support for specifying the default Java executable for running Ant (and MPS).

### Changed

- `generateBuildscripts` task now runs under MPS environment rather than IDEA to improve speed.

## 1.7.0

### Added

- Support for MPS 2022.3 and above. Needs a new dependency, `de.itemis.mps.build-backends:launcher`, available from
  the [itemis Nexus](https://artifacts.itemis.cloud/repository/maven-mps), as well as
  from [GitHub Packages](https://github.com/mbeddr/mps-build-backends/packages/1947539).

## 1.6.0

### Added
- Methods on `StubConfiguration` to create, add, and configure a project dependency.

### Changed
- Corrected types of `StubConfiguration#dependency()` overloads.

## 1.5.0

### Added
- Officially expose and document the `com.specificlanguages.RunAntScript` task.
- Add `mpsDefaults` extension (`com.specificlanguages.MpsDefaultsExtension`) to make some previously hardcoded settings 
  configurable.

### Changed
- The Ant classpath now uses an explicitly named configuration (`ant`) to support customizing it.
- Hide unintentionally public but undocumented classes and functions.

### Fixed
- Task inputs for `generateBuildscript` and `assemble` previously included files under the project build directory. 
  These files are now correctly excluded.

## 1.4.0

### Changed

- Use Gradle artifact transforms for downloading and unzipping the MPS distribution to enable its sharing among 
  multiple projects.

## 1.3.0

### Changed

- Use mps-build-backends to support MPS 2021.1.4 and above.
