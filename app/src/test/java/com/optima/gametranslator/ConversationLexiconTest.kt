package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class ConversationLexiconTest {
    private val lexicon=ConversationLexicon(mapOf("cover me" to "Beni koru", "what's up" to "Ne haber"))
    @Test fun exactPhraseAcceptsPunctuationAndReverseDirection() {
        assertEquals("Beni koru",lexicon.exact("  COVER ME! ",false))
        assertEquals("cover me",lexicon.exact("Beni koru.",true))
        assertEquals("Ne haber",lexicon.exact("what’s up?",false))
    }
    @Test fun slangCannotRewriteUnrelatedSentences() {
        assertNull(lexicon.exact("please cover me with a blanket",false))
        assertEquals("the wannabe musician",lexicon.normalizeEnglish("the wannabe musician"))
    }
    @Test fun contractionsAreExpandedBeforeTranslation() {
        assertEquals("I want to go but I have to wait",lexicon.normalizeEnglish("I wanna go but I gotta wait"))
        assertEquals("let me see",lexicon.normalizeEnglish("lemme see"))
    }
}
