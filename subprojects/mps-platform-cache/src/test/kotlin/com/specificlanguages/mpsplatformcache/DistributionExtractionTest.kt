package com.specificlanguages.mpsplatformcache

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Timeout(20)
class DistributionExtractionTest {
    private fun newExtraction(lockTimeout: Duration = Duration.ofMinutes(10)) =
        DistributionExtraction(lockTimeout)

    @Test
    fun `extraction replaces read-only output without following symlinks`(@TempDir root: Path) {
        assumeTrue(org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX))
        val outside = Files.createDirectory(root.resolve("outside"))
        Files.writeString(outside.resolve("keep"), "untouched")
        val directory = Files.createDirectory(root.resolve("distribution"))
        val nested = Files.createDirectory(directory.resolve("nested"))
        val partial = Files.writeString(nested.resolve("partial"), "incomplete")
        Files.createSymbolicLink(directory.resolve("linked-directory"), outside)
        Files.createSymbolicLink(directory.resolve("dangling"), root.resolve("missing"))
        assertTrue(partial.toFile().setWritable(false))
        assertTrue(nested.toFile().setWritable(false))
        try {
            newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
                Files.list(destination).use { assertEquals(0, it.count()) }
                Files.writeString(destination.resolve("content"), "complete")
            }
            assertEquals("untouched", Files.readString(outside.resolve("keep")))
            assertEquals("complete", Files.readString(directory.resolve("content")))
            assertTrue(Files.exists(directory.resolve(".complete")))
        } finally {
            if (Files.exists(nested)) nested.toFile().setWritable(true)
            if (Files.exists(partial)) partial.toFile().setWritable(true)
        }
    }

    @Test
    fun `incomplete extraction containing read-only files is replaced`(@TempDir root: Path) {
        val directory = Files.createDirectory(root.resolve("distribution"))
        val partial = Files.writeString(directory.resolve("read-only"), "incomplete")
        assertTrue(partial.toFile().setWritable(false), "Fixture must be read-only")
        try {
            newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
                Files.list(destination).use { assertEquals(0, it.count()) }
                Files.writeString(destination.resolve("content"), "complete")
            }
            assertTrue(Files.exists(directory.resolve(".complete")))
            assertEquals("complete", Files.readString(directory.resolve("content")))
        } finally {
            if (Files.exists(partial)) partial.toFile().setWritable(true)
        }
    }

    @Test
    fun `successful extraction may include its own completion marker`(@TempDir root: Path) {
        val directory = root.resolve("distribution")
        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
            Files.writeString(destination.resolve(".complete"), "archive marker")
        }
        assertEquals("archive marker", Files.readString(directory.resolve(".complete")))
    }

    @Test
    fun `waiting caller rechecks completion after the extractor releases the lock`(@TempDir root: Path) {
        val directory = root.resolve("distribution")
        val archive = root.resolve("archive")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = Worker {
            newExtraction().ensureExtracted(archive, directory) { _, destination ->
                Files.writeString(destination.resolve("content"), "complete")
                entered.countDown()
                assertTrue(release.await(10, TimeUnit.SECONDS))
            }
        }
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            assertFalse(Files.exists(directory.resolve(".complete")))
            Worker {
                newExtraction().ensureExtracted(archive, directory) { _, _ ->
                    fail("The waiting caller must reuse the completed extraction")
                }
            }.use { waiter ->
                waiter.awaitPolling()
                assertFalse(Files.exists(directory.resolve(".complete")))
                release.countDown()
                holder.awaitSuccess()
                waiter.awaitSuccess()
            }
            assertEquals("complete", Files.readString(directory.resolve("content")))
        } finally {
            release.countDown()
            holder.close()
        }
    }

    @Test
    fun `interrupting a waiting caller leaves the lock holder and partial output alone`(@TempDir root: Path) {
        val directory = Files.createDirectory(root.resolve("distribution"))
        val partial = Files.writeString(directory.resolve("partial"), "incomplete")
        FileChannel.open(root.resolve("distribution.lock"), CREATE, WRITE).use { channel ->
            channel.lock().use { lock ->
                Worker {
                    assertThrows(InterruptedException::class.java) {
                        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, _ ->
                            fail("Interrupted waiter must not extract")
                        }
                    }
                }.use { waiter ->
                    waiter.awaitPolling()
                    waiter.interrupt()
                    waiter.awaitSuccess()
                }
                assertTrue(lock.isValid)
                assertEquals("incomplete", Files.readString(partial))
                assertFalse(Files.exists(directory.resolve(".complete")))
            }
        }
        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, _ -> }
        assertTrue(Files.exists(directory.resolve(".complete")))
    }

    @Test
    fun `cleanup removes symlinks without deleting their targets`(@TempDir root: Path) {
        val outside = Files.createDirectory(root.resolve("outside"))
        Files.writeString(outside.resolve("keep"), "untouched")
        val directory = Files.createDirectory(root.resolve("distribution"))
        try {
            Files.createSymbolicLink(directory.resolve("linked-directory"), outside)
            Files.createSymbolicLink(directory.resolve("linked-file"), outside.resolve("keep"))
            Files.createSymbolicLink(directory.resolve("dangling"), root.resolve("missing"))
        } catch (e: UnsupportedOperationException) {
            assumeTrue(false, "Symbolic links are unsupported: $e")
        } catch (e: IOException) {
            assumeTrue(false, "Cannot create symbolic links on this filesystem: $e")
        }
        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
            Files.list(destination).use { assertEquals(0, it.count()) }
        }
        assertEquals("untouched", Files.readString(outside.resolve("keep")))
    }

    @Test
    fun `cleanup replaces a symlink at the extraction root without following it`(@TempDir root: Path) {
        val outside = Files.createDirectory(root.resolve("outside"))
        Files.writeString(outside.resolve("keep"), "untouched")
        val directory = root.resolve("distribution")
        try {
            Files.createSymbolicLink(directory, outside)
        } catch (e: UnsupportedOperationException) {
            assumeTrue(false, "Symbolic links are unsupported: $e")
        } catch (e: IOException) {
            assumeTrue(false, "Cannot create symbolic links on this filesystem: $e")
        }
        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
            assertFalse(Files.isSymbolicLink(destination))
            Files.list(destination).use { assertEquals(0, it.count()) }
        }
        assertEquals("untouched", Files.readString(outside.resolve("keep")))
    }

    @Test
    fun `another process owns extraction until completion is published`(@TempDir root: Path) {
        val directory = root.resolve("distribution")
        startExtractionProcess(root).use { child ->
            child.awaitExtraction()
            assertFalse(Files.exists(directory.resolve(".complete")))
            Worker {
                newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, _ ->
                    fail("The waiting caller must reuse the child process's extraction")
                }
            }.use { waiter ->
                waiter.awaitPolling()
                assertEquals("child output", Files.readString(directory.resolve("partial")))
                child.finishExtraction()
                waiter.awaitSuccess()
            }
            assertTrue(Files.exists(directory.resolve(".complete")))
            assertEquals("child completed", Files.readString(directory.resolve("content")))
        }
    }

    @Test
    fun `terminated extractor leaves incomplete output that the next process lock holder replaces`(@TempDir root: Path) {
        val directory = root.resolve("distribution")
        startExtractionProcess(root).use { child ->
            child.awaitExtraction()
            child.terminate()
            assertFalse(Files.exists(directory.resolve(".complete")))
            assertEquals("child output", Files.readString(directory.resolve("partial")))
            assertTrue(Files.exists(root.resolve("distribution.lock")))
            newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, destination ->
                Files.list(destination).use { assertEquals(0, it.count()) }
                Files.writeString(destination.resolve("content"), "recovered")
            }
            assertTrue(Files.exists(directory.resolve(".complete")))
            assertEquals("recovered", Files.readString(directory.resolve("content")))
        }
    }

    @Test
    fun `lock timeout does not limit extraction duration`(@TempDir root: Path) {
        val directory = root.resolve("distribution")
        newExtraction(Duration.ofMillis(1)).ensureExtracted(root.resolve("archive"), directory) { _, _ ->
            assertThrows(ExtractionLockTimeoutException::class.java) {
                newExtraction(Duration.ofMillis(150)).ensureExtracted(root.resolve("archive"), directory) { _, _ ->
                    fail("Contending extraction must time out")
                }
            }
            assertFalse(Files.exists(directory.resolve(".complete")))
        }
        assertTrue(Files.exists(directory.resolve(".complete")))
    }

    @Test
    fun `interrupted caller does not start extraction`(@TempDir root: Path) {
        Thread.currentThread().interrupt()
        try {
            assertThrows(InterruptedException::class.java) {
                newExtraction().ensureExtracted(root.resolve("archive"), root.resolve("distribution")) { _, _ ->
                    fail("Interrupted caller must not extract")
                }
            }
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `a held lock times out without touching incomplete output`(@TempDir root: Path) {
        val directory = Files.createDirectory(root.resolve("distribution"))
        val partial = Files.writeString(directory.resolve("partial"), "keep until lock acquired")
        FileChannel.open(root.resolve("distribution.lock"), CREATE, WRITE).use { channel ->
            channel.lock().use {
                val failure = assertThrows(ExtractionLockTimeoutException::class.java) {
                    newExtraction(Duration.ofMillis(150)).ensureExtracted(root.resolve("archive"), directory) { _, _ ->
                        fail("Extraction must wait for the lock")
                    }
                }
                assertTrue(failure.message.contains(directory.toAbsolutePath().toString()))
                assertEquals("keep until lock acquired", Files.readString(partial))
                assertFalse(Files.exists(directory.resolve(".complete")))
            }
        }
        newExtraction().ensureExtracted(root.resolve("archive"), directory) { _, _ -> }
        assertTrue(Files.exists(directory.resolve(".complete")))
        assertTrue(Files.exists(root.resolve("distribution.lock")))
    }

    @Test
    fun `failed extraction leaves incomplete output for the next attempt to replace`(@TempDir root: Path) {
        val archive = root.resolve("distribution.zip")
        val directory = root.resolve("cache/distribution")
        val failure = IllegalStateException("Extraction failed")
        val extraction = newExtraction()
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            extraction.ensureExtracted(archive, directory) { _, destination ->
                Files.createDirectories(destination.resolve("nested"))
                Files.writeString(destination.resolve("nested/partial"), "incomplete")
                throw failure
            }
        })
        assertFalse(Files.exists(directory.resolve(".complete")))
        assertTrue(Files.exists(directory.resolve("nested/partial")))
        extraction.ensureExtracted(archive, directory) { _, destination ->
            Files.list(destination).use { assertEquals(0, it.count()) }
            Files.writeString(destination.resolve("content"), "complete")
        }
        assertTrue(Files.exists(directory.resolve(".complete")))
        assertEquals("complete", Files.readString(directory.resolve("content")))
    }

    @Test
    fun `successful extraction publishes completion and is reused`(@TempDir root: Path) {
        val archive = Files.writeString(root.resolve("distribution.zip"), "archive")
        val directory = root.resolve("cache/distribution")
        val extraction = newExtraction()
        extraction.ensureExtracted(archive, directory) { input, destination ->
            assertEquals(archive, input)
            assertEquals(directory, destination)
            assertTrue(Files.isDirectory(destination))
            assertFalse(Files.exists(destination.resolve(".complete")))
            Files.copy(input, destination.resolve("content"))
        }
        assertTrue(Files.exists(directory.resolve(".complete")))
        FileChannel.open(directory.resolveSibling("distribution.lock"), CREATE, WRITE).use { channel ->
            channel.lock().use {
                newExtraction(Duration.ZERO).ensureExtracted(archive, directory) { _, _ ->
                    fail("Completed distribution must be reused without waiting for the lock")
                }
            }
        }
        assertEquals("archive", Files.readString(directory.resolve("content")))
    }

    private fun startExtractionProcess(root: Path): ExtractionChild {
        val javaName = if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"
        val java = Path.of(System.getProperty("java.home"), "bin", javaName).toString()
        val classpath = listOf(ExtractionLockHolder::class.java, Unit::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).path }
            .distinct().joinToString(File.pathSeparator)
        val process = ProcessBuilder(java, "-cp", classpath, ExtractionLockHolder::class.java.name,
            root.resolve("distribution").toString())
            .redirectError(root.resolve("child-errors.log").toFile())
            .start()
        return ExtractionChild(process, root.resolve("child-errors.log"))
    }

    private class ExtractionChild(private val process: Process, private val errors: Path) : AutoCloseable {
        private val ready = FutureTask { process.inputStream.bufferedReader().readLine() }
        private val reader = thread(name = "extraction-child-output") { ready.run() }

        fun awaitExtraction() {
            assertEquals("EXTRACTING", ready.get(10, TimeUnit.SECONDS), Files.readString(errors))
        }

        fun finishExtraction() {
            process.outputStream.close()
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Child extraction did not finish")
            assertEquals(0, process.exitValue(), Files.readString(errors))
        }

        fun terminate() {
            process.destroyForcibly()
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Child extractor did not terminate")
        }

        override fun close() {
            terminate()
            process.inputStream.close()
            process.outputStream.close()
            reader.join(10_000)
            assertFalse(reader.isAlive, "Child output reader did not stop")
        }
    }

    private class Worker(action: () -> Unit) : AutoCloseable {
        private val result = FutureTask(action)
        private val worker = thread(name = "distribution-extraction-test") { result.run() }

        fun awaitPolling() {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (worker.state != Thread.State.TIMED_WAITING && !result.isDone && System.nanoTime() < deadline) {
                Thread.yield()
            }
            if (result.isDone) result.get()
            assertEquals(Thread.State.TIMED_WAITING, worker.state, "Caller must be waiting for the held lock")
        }

        fun interrupt() = worker.interrupt()

        fun awaitSuccess() { result.get(10, TimeUnit.SECONDS) }

        override fun close() {
            worker.interrupt()
            worker.join(10_000)
            assertFalse(worker.isAlive, "Extraction worker did not stop")
        }
    }
}
