# Deprecation warning

This plugin is now deprecated in favor of the [mps-platform-cache](../mps-platform-cache/README.md) plugin (also found
in this repository) and should not be used.

The motivation behind this plugin was to share an MPS and JBR distribution among multiple, even unrelated projects. This
was to be achieved by implementing a Gradle artifact transform and leaving caching to Gradle. However, it turns out that
the cache key of an artifact transform is computed from the complete classpath of the buildscript, not just of the
transform. In practice this makes sharing unlikely, as documented by IntelliJ Platform Gradle Plugin which ran into a
similar [issue](https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1601).

The underlying issue on the Gradle side, [gradle/gradle#30968](https://github.com/gradle/gradle/issues/30968), is listed with no plans for a fix in the near
future. To fix the problem and implement proper sharing, IntelliJ Platform Gradle Plugin
switched from using artifact transforms to manual extraction. I am taking a similar approach and deprecating this plugin
in favor of mps-platform-cache which implements the extraction of MPS and JBR distributions into a shared directory.

# Usage

1. Declare a configuration with the dependency on an MPS distribution:

   ```kotlin
   val mpsConfiguration = configurations.create("mps")
   dependencies {
     mpsConfiguration("com.jetbrains:mps:2021.1.4")
   }
   ```

2. Use `ArtifactTransforms.getMpsRoot(mpsConfiguration)` in Kotlin to obtain the directory containing the unpacked
   distribution.

Using an MPS-based generic RCP build instead of MPS will also work.
