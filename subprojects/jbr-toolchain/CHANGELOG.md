# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.2

### Added

- Support for Gradle 8.13.

### Fixed

- Exception when running on Gradle 8.13:
  ```
  java.lang.NoSuchMethodError: 'org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec org.gradle.jvm.toolchain.internal.SpecificInstallationToolchainSpec.fromJavaHome(org.gradle.api.model.ObjectFactory, java.io.File)'
  ```

## 1.0.1

### Changed

- Improved the error message when the `jbr` configuration does not resolve to exactly one file.

## 1.0.0

- Initial version
