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
