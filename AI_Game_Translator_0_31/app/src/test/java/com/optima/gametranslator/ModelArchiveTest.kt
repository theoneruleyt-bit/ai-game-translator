package com.optima.gametranslator

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class ModelArchiveTest {
    @Test fun rejectsTraversalInsideRoot() {
        val base=Files.createTempDirectory("vosk-path").toFile()
        try {
            try { ModelArchive.destination(base,"${ModelArchive.ROOT}/../../escape");fail("Traversal accepted") }
            catch (_: IllegalArgumentException) { }
        } finally { base.deleteRecursively() }
    }
    @Test fun rejectsUnrelatedRoot() {
        val base=Files.createTempDirectory("vosk-root").toFile()
        try {
            try { ModelArchive.destination(base,"other/model");fail("Other root accepted") }
            catch (_: IllegalArgumentException) { }
        } finally { base.deleteRecursively() }
    }
    @Test fun incompleteModelIsRejected() {
        val base=Files.createTempDirectory("vosk-incomplete").toFile()
        try {
            try { ModelArchive.unpack(ByteArrayInputStream(archive(mapOf("conf/model.conf" to "x"))),base) { false };fail("Incomplete model accepted") }
            catch (_: IllegalArgumentException) { }
        } finally { base.deleteRecursively() }
    }
    @Test fun expectedFilesAreExtracted() {
        val base=Files.createTempDirectory("vosk-valid").toFile()
        val files=listOf("am/final.mdl","conf/model.conf","graph/HCLr.fst","graph/Gr.fst").associateWith { "data" }
        try {
            ModelArchive.unpack(ByteArrayInputStream(archive(files)),base) { false }
            files.keys.forEach { assertEquals("data",File(base,"${ModelArchive.ROOT}/$it").readText()) }
        } finally { base.deleteRecursively() }
    }
    @Test fun alternateStaticGraphAndPackRootAreSupported() {
        val base=Files.createTempDirectory("vosk-static").toFile()
        val root=SpeechPack.TURKISH.root
        val bytes=ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            listOf("am/final.mdl","conf/model.conf","graph/HCLG.fst").forEach {
                zip.putNextEntry(ZipEntry("$root/$it"));zip.write("data".toByteArray());zip.closeEntry()
            }
        }
        try {
            ModelArchive.unpack(ByteArrayInputStream(bytes.toByteArray()),base,root) { false }
            assertTrue(File(base,"$root/graph/HCLG.fst").isFile)
        } finally { base.deleteRecursively() }
    }
    @Test fun expandedLimitIsEnforced() {
        val base=Files.createTempDirectory("vosk-limit").toFile()
        try {
            try { ModelArchive.unpack(ByteArrayInputStream(archive(mapOf("am/final.mdl" to "data"))),base,expandedLimit=2) { false };fail("Limit ignored") }
            catch (_: IllegalArgumentException) { }
        } finally { base.deleteRecursively() }
    }
    @Test fun canceledExtractionStops() {
        val base=Files.createTempDirectory("vosk-cancel").toFile()
        try {
            try { ModelArchive.unpack(ByteArrayInputStream(archive(mapOf("am/final.mdl" to "data"))),base) { true };fail("Cancellation ignored") }
            catch (_: IllegalStateException) { }
        } finally { base.deleteRecursively() }
    }
    private fun archive(files: Map<String,String>): ByteArray {
        val bytes=ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            files.forEach { (name,text) ->
                zip.putNextEntry(ZipEntry("${ModelArchive.ROOT}/$name"));zip.write(text.toByteArray());zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
