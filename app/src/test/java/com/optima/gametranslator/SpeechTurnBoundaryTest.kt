package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SpeechTurnBoundaryTest {
 @Test fun silenceCannotInventATurn() {
  val b=SpeechTurnBoundary()
  repeat(100) { b.observe(false,it*50L);assertEquals("",b.accept("thank you for watching",it*50L)) }
  assertFalse(b.mustEnd(10000))
 }
 @Test fun lateDecoderWordsFreezeAfterSpeechStops() {
  val b=SpeechTurnBoundary();b.observe(true,1000)
  assertEquals("take cover",b.accept("take cover",1200))
  assertEquals("take cover now",b.accept("take cover now",1700))
  assertEquals("take cover now",b.accept("take cover now thank you thank you",1800))
  assertTrue(b.mustEnd(2100))
 }
 @Test fun shortPauseAndRealRepeatedDialogueRemainAllowed() {
  val b=SpeechTurnBoundary();b.observe(true,1000);b.accept("go",1100)
  assertFalse(b.mustEnd(1500));b.observe(true,1600)
  assertEquals("go go",b.accept("go go",1650));assertFalse(b.mustEnd(2000))
  b.reset();assertEquals("",b.accept("go go",3000))
  b.observe(true,3100);assertEquals("go go",b.accept("go go",3200))
 }
 @Test fun silenceAndQuantizationAreNotSpeechButQuietSignalIsAllowed() {
  assertFalse(SpeechTurnBoundary.hasSignal(FloatArray(800)))
  assertFalse(SpeechTurnBoundary.hasSignal(FloatArray(800) { 1f/32768 }))
  assertTrue(SpeechTurnBoundary.hasSignal(FloatArray(800) { if(it%2==0).0002f else -.0002f }))
 }
 @Test fun continuousSpeechHasNoArbitraryTimeLimit() {
  val b=SpeechTurnBoundary()
  repeat(1000) { val t=it*50L;b.observe(true,t);assertEquals("line $it",b.accept("line $it",t));assertFalse(b.mustEnd(t)) }
 }
 @Test fun recognizerCanRetractABlankHypothesisBeforeFreeze() {
  val b=SpeechTurnBoundary();b.observe(true,1000);b.accept("wrong word",1100)
  assertEquals("",b.accept("",1200));assertEquals("",b.accept("invented late words",2000))
 }
}
