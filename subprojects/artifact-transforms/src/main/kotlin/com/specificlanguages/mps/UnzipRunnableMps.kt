package com.specificlanguages.mps;

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.support.unzipTo
import java.io.File

/**
 * Unzips input artifact into 'mps' directory of the transformation outputs to make mps runnable
 */
abstract class UnzipRunnableMps : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputDir = outputs.dir("mps")

        // unzip
        unzipTo(outputDir, inputFile)

        // move the bin files
        val osSpecificBinFiles = File(outputDir, "bin/" + when (OperatingSystem.current()) {
                OperatingSystem.WINDOWS -> "win"
                OperatingSystem.LINUX -> "linux"
                OperatingSystem.MAC_OS -> throw UnsupportedOperationException("Unsupported operating system: ${OperatingSystem.current()}")
                else -> throw UnsupportedOperationException("Unsupported operating system: ${OperatingSystem.current()}")
        })
        osSpecificBinFiles.listFiles().forEach { file ->
            file.copyTo(File(outputDir.toString(), "bin/${file.name}"), overwrite = true)
        }

        // set the executable flag
        File(outputDir, "bin/mps.sh").setExecutable(true)
        File(outputDir, "bin/mps.exe").setExecutable(true)

        // magic version number which introduces the 'lib/jna/$arch' pattern
        val mpsVersion =
            File(outputDir, "build.txt").readText().split(".").first().lowercase().replace("mps-", "").toInt() >= 223
        if (mpsVersion) {
            // move the JNA files
            val osSpecificJnaFiles: File? = File(outputDir, "lib/jna/" + when (System.getProperty("os.arch")) {
                    "aarch64" -> "aarch64"
                    "x86_64" -> "amd64"
                    "amd64" -> "amd64"
                    else -> throw UnsupportedOperationException("Unsupported architecture: ${System.getProperty("os.arch")}")
            })

            osSpecificJnaFiles?.listFiles()?.forEach { file ->
                file.copyTo(File(outputDir.toString(), "lib/jna/${file.name}"), overwrite = true)
            }
        }
    }
}
