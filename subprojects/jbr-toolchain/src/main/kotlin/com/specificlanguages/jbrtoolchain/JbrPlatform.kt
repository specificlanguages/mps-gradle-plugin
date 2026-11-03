package com.specificlanguages.jbrtoolchain

import java.io.File

/**
 * An operating system and architecture combination for which a JBR distribution is published.
 *
 * Use the constants in the companion object together with [JbrToolchainExtension.selectPlatform].
 */
class JbrPlatform private constructor(internal val os: String, internal val arch: String) {
    /**
     * The classifier of the JBR distribution archive for this platform, e.g. `osx-aarch64`.
     */
    val classifier: String = "${os}-${arch}"

    internal fun getJavaHomeFromExtractedDirectory(root: File) =
        when (os) {
            OSX -> root.resolve("Contents/Home")
            else -> root
        }

    override fun equals(other: Any?): Boolean = other is JbrPlatform && other.classifier == classifier

    override fun hashCode(): Int = classifier.hashCode()

    override fun toString(): String = classifier

    companion object {
        private const val WINDOWS = "windows"
        private const val LINUX = "linux"
        private const val OSX = "osx"
        private const val X64 = "x64"
        private const val AARCH64 = "aarch64"

        @JvmField
        val WINDOWS_X64 = JbrPlatform(WINDOWS, X64)

        @JvmField
        val WINDOWS_AARCH64 = JbrPlatform(WINDOWS, AARCH64)

        @JvmField
        val LINUX_X64 = JbrPlatform(LINUX, X64)

        @JvmField
        val LINUX_AARCH64 = JbrPlatform(LINUX, AARCH64)

        @JvmField
        val OSX_X64 = JbrPlatform(OSX, X64)

        @JvmField
        val OSX_AARCH64 = JbrPlatform(OSX, AARCH64)

        internal fun current(): JbrPlatform = JbrPlatform(currentOs(), currentArch())

        private fun currentOs(): String = System.getProperty("os.name").let {
            val osName = it.lowercase()
            when {
                osName.contains("windows") -> WINDOWS
                osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx") -> OSX
                osName.contains("linux") -> LINUX
                else -> throw IllegalStateException("Unsupported value of os.name system property: $it")
            }
        }

        private fun currentArch(): String = System.getProperty("os.arch").let {
            when (it) {
                "x86_64", "amd64" -> X64
                "aarch64", "arm64" -> AARCH64
                else -> throw IllegalStateException("Unsupported value of os.arch system property: $it")
            }
        }
    }
}
