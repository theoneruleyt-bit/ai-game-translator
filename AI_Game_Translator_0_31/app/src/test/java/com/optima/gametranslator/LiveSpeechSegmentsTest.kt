package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class LiveSpeechSegmentsTest {
    @Test fun previewNeverCommitsHalfASentence() {
        val s=LiveSpeechSegments()
        val text="we need to get out of here"
        for(now in listOf(0L,180L,450L,900L)) {
            val update=s.update(text,false,now)
            assertTrue(update.chunks.isEmpty())
            assertEquals(text,update.preview)
        }
        assertEquals(listOf(text),s.update(text,true,1000).chunks)
    }
    @Test fun aRevisedFinalKeepsWordsInsertedIntoThePrefix() {
        val s=LiveSpeechSegments()
        s.update("we need get out here",false,0)
        s.update("we need get out here",false,700)
        val final="we really need to get out of here now"
        assertEquals(listOf(final),s.update(final,true,900).chunks)
    }
    @Test fun shortPhraseAndRepeatedDialogueAreKept() {
        val s=LiveSpeechSegments()
        assertEquals(listOf("watch out"),s.update("watch out",true,0).chunks)
        assertEquals(listOf("watch out"),s.update("watch out",true,100).chunks)
    }
    @Test fun emptyFinalCannotCutTheNextUtterance() {
        val s=LiveSpeechSegments()
        s.update("one two three four five",false,0)
        s.update("one two three four five",false,400)
        assertTrue(s.update("",true,500).chunks.isEmpty())
        assertEquals(listOf("brand new words"),s.update("brand new words",true,700).chunks)
    }
    @Test fun aLongUtteranceRemainsComplete() {
        val s=LiveSpeechSegments()
        val text=(1..30).joinToString(" ") { "word$it" }
        assertTrue(s.update(text,false,0).chunks.isEmpty())
        assertTrue(s.update(text,false,1200).chunks.isEmpty())
        assertEquals(listOf(text),s.update(text,true,1400).chunks)
    }
}
