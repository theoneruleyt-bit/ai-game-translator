package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class LiveSpeechSegmentsTest {
    @Test fun emitsBeforeFinalAndOnlyRemainingTailAtFinal() {
        val s=LiveSpeechSegments()
        val text="we need to get out of here"
        assertTrue(s.update(text,false,0).chunks.isEmpty())
        assertTrue(s.update(text,false,180).chunks.isEmpty())
        assertEquals(listOf("we need to get out of"),s.update(text,false,450).chunks)
        assertEquals(listOf("here"),s.update(text,true,500).chunks)
    }
    @Test fun revisedTailIsNotCommittedPrematurely() {
        val s=LiveSpeechSegments()
        s.update("please open the door",false,0)
        s.update("please close the door",false,200)
        assertTrue(s.update("please close the door",false,300).chunks.isEmpty())
        assertEquals(listOf("please close the door"),s.update("please close the door",true,350).chunks)
    }
    @Test fun shortPhraseAndRepeatedDialogueAreKept() {
        val s=LiveSpeechSegments()
        assertEquals(listOf("watch out"),s.update("watch out",true,0).chunks)
        assertEquals(listOf("watch out"),s.update("watch out",true,100).chunks)
    }
    @Test fun emptyFinalResetsTheNextUtterance() {
        val s=LiveSpeechSegments()
        s.update("one two three four five",false,0)
        s.update("one two three four five",false,100)
        assertFalse(s.update("one two three four five",false,400).chunks.isEmpty())
        s.update("",true,500)
        assertEquals(listOf("brand new words"),s.update("brand new words",true,700).chunks)
    }
    @Test fun aLongStreamIsBoundedAndNotRepeatedAtFinal() {
        val s=LiveSpeechSegments()
        val text=(1..30).joinToString(" ") { "word$it" }
        s.update(text,false,0)
        val chunks=mutableListOf<String>()
        for(now in listOf(100L,300L,600L,900L,1200L))chunks.addAll(s.update(text,false,now).chunks)
        chunks.addAll(s.update(text,true,1400).chunks)
        assertEquals(text,chunks.joinToString(" "))
        assertTrue(chunks.dropLast(1).all { it.split(' ').size<=8 })
    }
}
