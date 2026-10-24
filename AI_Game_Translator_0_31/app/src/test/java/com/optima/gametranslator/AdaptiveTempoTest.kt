package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class AdaptiveTempoTest {
    @Test fun manualModePreservesRate() {
        assertEquals(.85f,AdaptiveTempo().choose("Merhaba",.85f,60000,60000,500,false),.001f)
    }
    @Test fun backlogAcceleratesWithinLimit() {
        val t=AdaptiveTempo();var rate=1f
        repeat(20) { rate=t.choose("Uzun bir konuşma",1f,60000,30000,500,true) }
        assertEquals(1.5f,rate,.001f)
    }
    @Test fun slowSourceCanSlowPlayback() {
        assertEquals(.85f,AdaptiveTempo().choose("Merhaba",1f,0,0,10000,true),.001f)
    }
    @Test fun changesAreGradual() {
        assertTrue(AdaptiveTempo().choose("Uzun replik",1f,60000,20000,500,true)<=1.201f)
    }
    @Test fun completedPlaybackCalibratesEstimate() {
        val t=AdaptiveTempo();val text="Merhaba arkadaşım";val before=t.estimateMillis(text)
        t.completed(text,1f,10000);assertTrue(t.estimateMillis(text)>before)
        val calibrated=t.estimateMillis(text);t.completed(text,1f,200000)
        assertEquals(calibrated,t.estimateMillis(text))
    }
 @Test fun liveBacklogCanUseVeryFastReadingButCameraLimitIsPreserved() {
  val t=AdaptiveTempo();var live=1f
  repeat(10) { live=t.choose("Bu uzun bir replik",1.1f,9000,8000,500,true,2f) }
  assertTrue(live>1.5f);assertTrue(live<=2f)
  val camera=AdaptiveTempo();var normal=1f
  repeat(10) { normal=camera.choose("Bu uzun bir replik",1.1f,9000,8000,500,true) }
  assertTrue(normal<=1.5f)
 }
}
