# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.1

### Changed

- The plugin now works with Gradle 9.6.1 where an internal `unzipTo` method was removed.
- `getMpsRoot` and `getJbrRoot` now require the configuration to resolve to a single artifact (rather than to contain a
  single declared dependency) and derive the cache location from that resolved artifact's coordinates. As a result the
  distribution is cached under its own coordinates even when reached transitively, e.g. through a marker artifact such as
  `com.jetbrains.mps:mps-jbr`.

## 1.0.0

- Initial version
