package com.specificlanguages.jbrtoolchain.internal

import java.io.File

internal class JbrOsArch(val os: String, val arch: String) {
    val classifier = "${os}-${arch}"

    internal fun getJavaHomeFromExtractedDirectory(root: File) =
        when (os) {
            OSX -> root.resolve("Contents/Home")
            else -> root
        }

    companion object {
        private const val OSX = "osx"

        internal fun current(): JbrOsArch = JbrOsArch(currentOs(), currentArch())

        private fun currentOs(): String = System.getProperty("os.name").let {
            val osName = it.lowercase()
            when {
                osName.contains("windows") -> "windows"
                osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx") -> OSX
                osName.contains("linux") -> "linux"
                else -> throw IllegalStateException("Unsupported value of os.name system property: $it")
            }
        }

        private fun currentArch(): String = System.getProperty("os.arch").let {
            when (it) {
                "x86_64", "amd64" -> "x64"
                "aarch64", "arm64" -> "aarch64"
                else -> throw IllegalStateException("Unsupported value of os.arch system property: $it")
            }
        }
    }
}
