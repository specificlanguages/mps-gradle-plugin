package com.specificlanguages.mpsplatformcache

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

/** Child JVM that simulates extraction while holding a real lock until stdin closes or the process is terminated. */
object ExtractionLockHolder {
    @JvmStatic
    fun main(args: Array<String>) {
        val destination = Path.of(args[0])
        val lockFile = destination.resolveSibling("${destination.fileName}.lock")
        FileChannel.open(lockFile, CREATE, WRITE).use { channel ->
            channel.lock().use {
                Files.createDirectories(destination)
                Files.writeString(destination.resolve("partial"), "child output")
                println("EXTRACTING")
                System.out.flush()
                System.`in`.read()
                Files.delete(destination.resolve("partial"))
                Files.writeString(destination.resolve("content"), "child completed")
                Files.createFile(destination.resolve(".complete"))
            }
        }
    }
}
