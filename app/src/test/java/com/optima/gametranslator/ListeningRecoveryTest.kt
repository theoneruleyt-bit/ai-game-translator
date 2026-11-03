package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class ListeningRecoveryTest {
    @Test fun optionalNativeFailurePreservesAllFollowingSpeech() {
        for(failure in listOf<Throwable>(IllegalStateException(),UnsatisfiedLinkError(),OutOfMemoryError())) {
            var releases=0;var warnings=0;var calls=0
            val punctuation=PunctuationFallback({ calls++;if(calls==1) "$it." else throw failure },{ releases++ },{ warnings++ })
            assertEquals("First sentence.",punctuation.apply("First sentence"))
            assertEquals("Second sentence",punctuation.apply("Second sentence"))
            assertEquals("Third sentence",punctuation.apply("Third sentence"))
            punctuation.close();punctuation.close()
            assertEquals(2,calls);assertEquals(1,releases);assertEquals(1,warnings)
        }
    }
    @Test fun failedNativeReleaseStillAllowsRecognition() {
        var warnings=0
        val punctuation=PunctuationFallback({ throw UnsatisfiedLinkError() },{ throw UnsatisfiedLinkError() },{ warnings++ })
        assertEquals("Keep listening",punctuation.apply("Keep listening"))
        assertEquals(1,warnings)
    }
    @Test(expected=ThreadDeath::class) fun terminationIsNeverSwallowed() {
        PunctuationFallback({ throw ThreadDeath() },{ },{ }).apply("stop")
    }
    @Test fun emptyEndpointClearsDraftButDoesNotInventFinalWords() {
        val events=RecognitionEvents();var draft="";val spoken=mutableListOf<String>()
        for(event in listOfNotNull(events.accept(" unfinished",false),events.accept("",true,"end-1"),events.accept("",true,"end-1"))) {
            draft=if(event.final) "" else event.text
            if(event.final&&event.text.isNotBlank())spoken.add(event.text)
        }
        assertEquals("",draft);assertTrue(spoken.isEmpty())
        assertNull(events.accept("   ",false))
    }
    @Test fun repeatedDialogueIsKeptWhileDuplicateDeliveryIsRejected() {
        val events=RecognitionEvents()
        assertEquals("Go!",events.accept("Go!",true,"a")?.text)
        assertNull(events.accept("Go!",true,"a"))
        assertEquals("Go!",events.accept("Go!",true,"b")?.text)
    }
    @Test fun busyEngineDoesNotSpendFailureRetriesOrReorderDialogue() {
        val queue=LineQueue();queue.offer("First",100);queue.offer("Second",200)
        var current=requireNotNull(queue.take());var elapsed=0L
        repeat(12) {
            val delay=requireNotNull(TranslationRetryPolicy.delay(true,current.attempts,elapsed))
            elapsed+=delay;current=current.copy(retryStartedAt=100)
        }
        assertEquals(0,current.attempts);assertEquals("First",current.text)
        assertEquals(100L,current.seenAt);assertEquals("Second",queue.take()?.text)
        assertNull(TranslationRetryPolicy.delay(true,current.attempts,6000))
        assertEquals(300L,TranslationRetryPolicy.delay(false,current.attempts,elapsed))
        current=current.copy(attempts=current.attempts+1)
        assertEquals(600L,TranslationRetryPolicy.delay(false,current.attempts,elapsed))
        assertNull(TranslationRetryPolicy.delay(false,2,elapsed))
    }
}
