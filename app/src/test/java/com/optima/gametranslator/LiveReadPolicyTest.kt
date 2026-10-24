package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class LiveReadPolicyTest {
 @Test fun sourceSilenceDoesNotDrainOldUnstartedSentences() {
  val p=LiveReadPolicy();p.observeSpeech(1000)
  assertTrue(p.allows(1500,2500,false));assertFalse(p.allows(1500,5000,false))
 }
 @Test fun runningSentenceIsAllowedToFinishWithoutBeingCut() {
  val p=LiveReadPolicy();p.observeSpeech(0)
  assertTrue(p.allows(0,20000,true));assertFalse(p.allows(0,20000,false))
 }
 @Test fun explicitResetDoesNotKeepOldSessionSpeechTime() {
  val p=LiveReadPolicy();p.observeSpeech(0);p.reset();assertTrue(p.allows(20000,20500,false))
 }
 @Test fun sameWordsWithDifferentTurnIdsAreRealRepetitions() {
  val g=FinalTurnGuard();assertTrue(g.accept("1:1"));assertFalse(g.accept("1:1"));assertTrue(g.accept("1:2"));assertTrue(g.accept("2:1"))
 }
}
