# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
