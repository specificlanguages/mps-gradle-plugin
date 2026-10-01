package com.specificlanguages.mpsplatformcache

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.time.Duration
import java.util.concurrent.TimeUnit

internal class ExtractionLockTimeoutException(override val message: String) : IOException(message)

internal class DistributionExtraction(
    private val lockTimeout: Duration = Duration.ofMinutes(10),
) {
    /**
     * Reuses a completed distribution or replaces incomplete output while holding its sibling file lock.
     * Calls [extract] synchronously under the lock and publishes completion only after it returns successfully.
     * Failed extraction leaves incomplete output for the next lock holder to replace.
     * [lockTimeout] limits lock acquisition only; extraction has no time limit.
     */
    fun ensureExtracted(archive: Path, directory: Path, extract: (archive: Path, destination: Path) -> Unit) {
        val completion = directory.resolve(".complete")
        if (Files.exists(completion)) return
        val lockFile = directory.resolveSibling("${directory.fileName}.lock")
        Files.createDirectories(lockFile.toAbsolutePath().parent)
        // Keep the lock file so waiting processes and new callers always lock the same file.
        FileChannel.open(lockFile, CREATE, WRITE).use { channel ->
            lockWithRetry(channel, directory).use {
                if (Files.exists(completion)) return
                if (Files.exists(directory, NOFOLLOW_LINKS)) {
                    deleteDistributionDirectory(directory)
                }
                Files.createDirectories(directory)
                extract(archive, directory)
                try {
                    Files.createFile(completion)
                } catch (_: FileAlreadyExistsException) {
                    // The archive may itself contain a completion marker.
                }
            }
        }
    }

    private fun lockWithRetry(channel: FileChannel, directory: Path): FileLock {
        val startedNanos = System.nanoTime()
        val timeoutNanos = lockTimeout.toNanos()
        while (true) {
            if (Thread.currentThread().isInterrupted) {
                throw InterruptedException("Interrupted while waiting to extract '$directory'")
            }
            try {
                channel.tryLock()?.let { return it }
            } catch (_: OverlappingFileLockException) {
                // tryLock returns null for other processes, but throws for overlapping locks in this JVM.
            }
            val elapsedNanos = System.nanoTime() - startedNanos
            if (elapsedNanos >= timeoutNanos) {
                throw ExtractionLockTimeoutException(
                    "Timed out waiting to extract '${directory.toAbsolutePath()}' after " +
                        "${TimeUnit.NANOSECONDS.toSeconds(elapsedNanos)}s (lock acquisition timeout: $lockTimeout)"
                )
            }
            TimeUnit.MILLISECONDS.sleep(100)
        }
    }
}

/**
 * Deletes incomplete extraction output without following symbolic links.
 * JBR extraction runs inside a Gradle ValueSource, where FileSystemOperations is not available for
 * injection. NIO deletion lets both ValueSource and plugin callers use the same extraction implementation.
 */
private fun deleteDistributionDirectory(directory: Path) {
    Files.walkFileTree(directory, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            // Directory write permission is needed to remove its entries on Unix.
            dir.toFile().setWritable(true)
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            // Windows requires read-only files to be made writable before deletion.
            if (attrs.isRegularFile) file.toFile().setWritable(true)
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            if (exc != null) throw exc
            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }
    })
}
