# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Changed

- The plugin now works with Gradle 9.6.1 where an internal `unzipTo` method was removed.
- `getJbrRoot` now derives the cache location from the resolved distribution artifact rather than the declared
  dependency, so the JBR is cached under its own coordinates even when reached through a marker artifact.

## 1.0.0

- Initial version
