package com.specificlanguages.mps.internal

/**
 * Names of configurations defined by the plugin.
 */
internal class ConfigurationNames {
    companion object {
        /**
         * MPS libraries that are transitively visible to the consumers of this project (dependency scope).
         *
         * MPS does not distinguish between generation-only, runtime-only and API dependencies, but making this
         * distinction at least in this plugin could prove useful for third-party tooling.
         */
        const val API = "api"

        /**
         * MPS libraries that are only used for testing (dependency scope). Invisible to the consumers of this project.
         */
        const val TEST_IMPLEMENTATION = "testImplementation"

        /**
         * All MPS libraries that have to be downloaded and extracted to build this project and run its tests
         * (resolvable).
         */
        const val MPS_LIBRARIES = "mpsLibraries"

        /**
         * The packaged artifacts of this project along with all its non-test dependencies (consumable).
         *
         * Gradle Java plugins distinguish between 'API elements' (required to compile and run consumer projects) and
         * 'runtime elements' (only necessary to compile consumer projects). As noted in [API], MPS does not make this
         * distinction so we only have 'API elements' for now, but we may eventually introduce 'runtime elements' for
         * tooling.
         */
        const val API_ELEMENTS = "apiElements"

        /**
         * The generic MPS distribution in a zip (resolvable, dependency scope).
         */
        const val MPS = "mps"

        /**
         * The `execute-generators` backend from mps-build-backends (resolvable, dependency scope). This is a somewhat
         * internal configuration with a default dependency that can be overridden if the project needs to use a custom
         * version of the backend.
         */
        const val EXECUTE_GENERATORS = "executeGenerators"
    }
}