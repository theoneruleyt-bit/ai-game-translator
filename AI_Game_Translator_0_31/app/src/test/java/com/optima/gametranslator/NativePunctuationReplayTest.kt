package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/** Actual Linux Sherpa 1.13.8 model predictions, replayed through the Android-independent cursor policy. */
class NativePunctuationReplayTest {
    private val predictions by lazy {
        val source=checkNotNull(javaClass.getResourceAsStream("/native-punctuation-0_23.tsv"))
        source.bufferedReader().useLines { rows -> rows.associate { row ->
            val columns=row.split('\t');val decoder=Base64.getDecoder()
            String(decoder.decode(columns[0]),Charsets.UTF_8) to String(decoder.decode(columns[1]),Charsets.UTF_8)
        } }
    }
    private fun normalized(text: String)=text.filter { it.isLetterOrDigit() }.lowercase(java.util.Locale.ROOT)
    private fun replay(text: String) {
        val words=text.split(' ');val stream=AdaptiveSpeechClauses();val spoken=mutableListOf<String>()
        val predict:(String)->String={ input -> checkNotNull(predictions[input]) { "Missing native fixture: $input" } }
        var now=0L
        for(n in 3..words.size step 3) {
            val partial=words.take(n).joinToString(" ")
            stream.update(partial,now,0,false,predict).filter { it.final }.forEach { spoken.add(it.text) }
            now+=350
        }
        assertTrue("Long uninterrupted speech should emit a meaning group before endpoint",spoken.isNotEmpty())
        stream.update(text,now+350,700,true,predict).filter { it.final }.forEach { spoken.add(it.text) }
        assertEquals("No replayed or missing words after the cumulative native endpoint",normalized(text),normalized(spoken.joinToString(" ")))
    }
    @Test fun continuousGameDialogueCommitsBeforeNativeEndAndPreservesAllWords() {
        replay("we need to find another way out the guards are coming down the stairs and we do not have much time i will meet you at the station bring the car around and wait by the gate do not leave without me")
    }
    @Test fun longSingleSpeakerNarrationAlsoCommitsWithoutAnySilence() {
        replay("we went to the station and the guard told us to wait but the train had already left i have been waiting for you for a long time and now we need to leave because the police are coming")
    }
}
