# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
