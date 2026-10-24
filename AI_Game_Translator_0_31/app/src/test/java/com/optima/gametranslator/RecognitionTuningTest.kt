package com.optima.gametranslator

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RecognitionTuningTest {
    @Test fun changingModesPreservesOriginalAndDoesNotDuplicateOptions() {
        val root=Files.createTempDirectory("speech-tuning").toFile()
        try {
            File(root,"conf").mkdirs()
            val original="--beam=10\n--endpoint.rule2.min-trailing-silence=0.5\n"
            val conf=File(root,"conf/model.conf");conf.writeText(original)
            RecognitionTuning.configure(root,0)
            assertTrue(conf.readText().contains("--endpoint.rule2.min-trailing-silence=0.35"))
            assertEquals(1,conf.readLines().count { it.startsWith("--endpoint.rule2.min-trailing-silence=") })
            RecognitionTuning.configure(root,1)
            assertTrue(conf.readText().contains("--endpoint.rule5.min-utterance-length=10.0"))
            assertTrue(conf.readText().contains("--beam=10"))
            RecognitionTuning.configure(root,2)
            assertTrue(conf.readText().contains("--endpoint.rule2.min-trailing-silence=0.6"))
            assertTrue(conf.readText().contains("--endpoint.rule5.min-utterance-length=20.0"))
            RecognitionTuning.configure(root,0)
            assertTrue(conf.readText().contains("--endpoint.rule5.min-utterance-length=6.0"))
            for(rule in 2..5) {
                assertEquals(1,conf.readLines().count { it.startsWith("--endpoint.rule$rule.") })
            }
            assertTrue(conf.readText().contains("--beam=10"))
            assertEquals(original,File(root,"conf/model.conf.original").readText())
        } finally { root.deleteRecursively() }
    }
}
