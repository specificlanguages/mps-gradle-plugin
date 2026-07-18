# mps-gradle-plugin

Gradle plugins for building [JetBrains MPS](https://www.jetbrains.com/mps/) projects.

## Modules

| Module | Description |
| --- | --- |
| [mps-gradle-plugin](subprojects/mps-gradle-plugin) | Package and publish MPS language/plugin libraries. |
| [jbr-toolchain](subprojects/jbr-toolchain) | Download a specific JetBrains Runtime (JBR) version and expose it as a JVM toolchain. |
| [mps-platform-cache](subprojects/mps-platform-cache) | Extract MPS and JBR distributions into a shared cache directory. Used internally by the other plugins. |
| [artifact-transforms](subprojects/artifact-transforms) | **Deprecated**, superseded by mps-platform-cache. |

Each module is versioned and released independently; see its own README for usage and its changelog for release
history.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for commit message conventions, versioning, and the release process.

## License

[Apache License 2.0](LICENSE)
