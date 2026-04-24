package com.specificlanguages.mps

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Installs the bundled mops CLI (`/com/specificlanguages/mps/mops` on the plugin classpath) into
 * [targetDir], default `$HOME/.local/bin`. The installed file embeds its version via a
 * `# mops-version: X` comment, so re-running this task against an existing install compares
 * the embedded version: equal → no-op, older → upgrade, newer → refuse (no implicit downgrade).
 */
abstract class InstallMops : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val targetDir: DirectoryProperty

    init {
        group = "build setup"
        description = "Installs the mops CLI to targetDir (default ~/.local/bin)."
    }

    @TaskAction
    fun install() {
        val dest = targetDir.get().asFile.resolve(SCRIPT_NAME)
        dest.parentFile?.mkdirs()

        val installing = version.get()
        val existing = readMopsVersion(dest)
        if (existing != null) {
            val cmp = compareVersions(existing, installing)
            when {
                cmp > 0 -> throw GradleException(
                    "Refusing to downgrade mops at $dest from $existing to $installing. " +
                        "Remove the file manually if this is intentional."
                )
                cmp == 0 -> {
                    logger.lifecycle("mops $installing already installed at $dest.")
                    return
                }
            }
        }

        val body = loadBundledScript().replace(VERSION_PLACEHOLDER, installing)
        dest.writeText(body)
        dest.setExecutable(true, /* ownerOnly = */ false)

        if (existing == null) {
            logger.lifecycle("Installed mops $installing to $dest.")
        } else {
            logger.lifecycle("Upgraded mops at $dest from $existing to $installing.")
        }
    }

    private fun loadBundledScript(): String =
        javaClass.getResourceAsStream(SCRIPT_RESOURCE)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw GradleException("Bundled mops script not found on plugin classpath at $SCRIPT_RESOURCE")

    companion object {
        private const val SCRIPT_NAME = "mops"
        private const val SCRIPT_RESOURCE = "/com/specificlanguages/mps/mops"
        private const val VERSION_PLACEHOLDER = "@VERSION@"

        // Matches the `# mops-version: X.Y.Z` marker embedded by install().
        private val VERSION_MARKER = Regex("""^# mops-version:\s*(\S+)""")

        internal fun readMopsVersion(file: File): String? {
            if (!file.isFile) return null
            return file.bufferedReader().useLines { lines ->
                lines.take(20).firstNotNullOfOrNull { line ->
                    VERSION_MARKER.find(line)?.groupValues?.get(1)
                }
            }
        }

        // Lexicographic over dot/dash/plus-separated numeric segments; non-numeric segments collate
        // as 0. Good enough for typical Gradle versions ("2.0.1", "2.1.0-SNAPSHOT"); refine if users
        // start using schemes that need strict semver precedence.
        internal fun compareVersions(a: String, b: String): Int {
            val aParts = a.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val bParts = b.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val n = maxOf(aParts.size, bParts.size)
            for (i in 0 until n) {
                val cmp = aParts.getOrElse(i) { 0 }.compareTo(bParts.getOrElse(i) { 0 })
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}
