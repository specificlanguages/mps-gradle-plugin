# MPS Gradle Plugin

A Gradle plugin to package and publish MPS plugins. Use it if you have developed a language in MPS and you want to make
it easy for other developers to use your language in their projects.

Publishing MPS-based IDEs (rich client platforms) is out of scope for this plugin.

This plugin also does not cover publishing to the JetBrains Plugins Repository.

## Compatibility

This plugin has been tested with the following combinations of Gradle and MPS:
* Gradle 5.6.2 and MPS 2019.1.5 (version 0.0.2),
* Gradle 5.6.2 and MPS 2019.2.4 (version 1.0.0).

## Conventions and Assumptions

To simplify the configuration the plugin makes certain assumptions and therefore your project must follow certain
conventions.

The following assumptions will hold if you let the MPS wizard generate your build solution and script:
* The MPS project directory is assumed to coincide with the Gradle project directory.
* The MPS project must contain a build model that is either named `build.mps` or matches `*.build.mps`. The build model
  must use default persistence and not file-per-root persistence. 
* The build model must generate a `build.xml` file in the project's root directory.
* The variable that specifies the location of MPS in the build script is called `mps_home`.

The following conventions are different from the defaults and require manual adjustment of the generated build script:
* The default layout should also include the build solution.
* Instead of creating a .zip of all modules or plugins the default layout should just collect them in the top-level
  folder:
  ```
  default layout:
    module com.mbeddr.mpsutil.common 
    module com.mbeddr.mpsutil.common.build
  ```
  or
  ```
  default layout:
    plugin com.mbeddr.mpsutil.common [auto packaging]
      <empty>
  ```
* Any dependencies will be put under `build/dependencies` folder and can be referenced from the project's base directory
  (without using any path variables).

## Sample Project

A sample project using the plugin can be found here: https://github.com/specificlanguages/mps-gradle-plugin-sample.

## Configuration

All code snippets below use Kotlin syntax for Gradle.

1. Apply the plugin:

    ```kotlin
    plugins {
        id("com.specificlanguages.mps") version "1.0.1"
    }
    ```

2. Add the itemis mbeddr repository and the JCenter or Maven Central repository to the project:

   ```kotlin
   repositories {
       maven(url = "https://projects.itemis.de/nexus/content/repositories/mbeddr")
       mavenCentral() // or jcenter()
   }
   ```
   
   The itemis mbeddr repository is used to download MPS as well as a small runner program to launch MPS from the command
   line. (The launcher is part of [mbeddr mps-gradle-plugin](https://github.com/mbeddr/mps-gradle-plugin).) Maven
   Central and JCenter repositories contain the Kotlin libraries that the launcher depends on.

3. Use the `mps` configuration to specify the MPS version to use:

   ```kotlin
   dependencies {
       "mps"("com.jetbrains:mps:2019.1.5")
   }
   ```

4. Include the dependencies necessary for generation into the `generation` configuration:

    ```kotlin
    dependencies {
        "generation"("de.itemis.mps:extensions:2019.1.1093.4f96363")
    }
    ```

5. For publishing apply the `maven-publish` plugin and use `from(components["mps"])`:

   ```kotlin
   plugins {
       ...
       `maven-publish`
   }
   ...
   publishing {
       publications {
           register<MavenPublication>("mpsPlugin") {
               from(components["mps"])
   
               // Put resolved versions of dependencies into POM files
               versionMapping { usage("default") { fromResolutionOf("generation") } }
           }
       }
   }
   ```

## Effects of Applying the Plugin

The plugin applies the [Gradle Base plugin](https://docs.gradle.org/current/userguide/base_plugin.html) which creates
a set of _lifecycle tasks_ such as `clean`, `assemble`, `check`, or `build`.

The plugin creates the following tasks:

* `setup`: unpacks all dependencies of the `generation` configuration into `build/dependencies`.
* `resolveMpsForGeneration`: downloads the MPS artifact specified by the `mps` configuration and unpacks it into
  `build/mps`.
* `generateBuildscript`: generates the build script using MPS. The build model location is detected automatically.
* `assembleMps`: runs `generate` and `assemble` targets of the generated Ant script. The `assemble`
  lifecycle task is set to depend on `assembleMps`.
* `checkMps`: runs the `check` target of the generated Ant script. The `check` lifecycle task is set to depend
  on `checkMps`.
* `package`: packages the modules built by `assembleMps` in a ZIP. The package is added to the `default` configuration
  (created by the Gradle Base plugin).

The plugin creates a software component named `mps` to represent the published code and adds the `default` configuration
to it.

The plugin modifies the `clean` task to delete MPS-generated directories: `source_gen`, `source_gen.caches`,
`classes_gen`, `tests_gen`, and `tests_gen.caches`. This is in addition to the default operation of `clean` task which
deletes the project's build directory (`build`).
