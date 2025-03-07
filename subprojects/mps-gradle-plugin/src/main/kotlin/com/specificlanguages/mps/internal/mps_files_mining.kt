package com.specificlanguages.mps.internal

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Functions for extracting information from MPS files
 */
private val MODEL_NAME_REGEX = Regex("""<model ref=".*\((.*)\)">""")
private val SOLUTION_NAME_REGEX = Regex("""<solution name="([^"]*)"""")

private const val XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

internal sealed class ParsingResult<out R> {
    data class Error(val message: String) : ParsingResult<Nothing>()
    data class Value<out R>(val value: R) : ParsingResult<R>()
}

internal fun readModelName(file: File): ParsingResult<String> = try {
    file.bufferedReader(Charsets.UTF_8).use {
        val xmlHeader = it.readLine()
        if (!XML_PROLOG.equals(xmlHeader, true)) {
            return ParsingResult.Error("XML prolog not found in file")
        }
        val header = it.readLine()
        val matchResult =
            MODEL_NAME_REGEX.find(header) ?: return ParsingResult.Error("model name regex did not match $header")

        return ParsingResult.Value(matchResult.groupValues[1])
    }
} catch (_: FileNotFoundException) {
    ParsingResult.Error("file not found")
} catch (io: IOException) {
    ParsingResult.Error("error reading file: ${io.message}")
}

internal fun readSolutionName(descriptor: File): ParsingResult<String> = try {
    descriptor.bufferedReader(Charsets.UTF_8).use {
        val xmlHeader = it.readLine()
        if (!XML_PROLOG.equals(xmlHeader, true)) {
            return ParsingResult.Error("XML prolog not found in file")
        }
        val header = it.readLine()
        val matchResult =
            SOLUTION_NAME_REGEX.find(header) ?: return ParsingResult.Error("solution name regex did not match $header")
        return ParsingResult.Value(matchResult.groupValues[1])
    }
} catch (_: FileNotFoundException) {
    ParsingResult.Error("file not found")
} catch (io: IOException) {
    ParsingResult.Error("error reading file: ${io.message}")
}