package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class RecentLinesTest {
 @Test fun oscillatingOcrDoesNotRepeatSpeech() {
  val r=RecentLines();assertTrue(r.shouldSubmit("Go now!",0));assertTrue(r.shouldSubmit("Go now? extra",200))
  assertFalse(r.shouldSubmit("GO NOW.",400))
 }
 @Test fun actualRepeatAfterBlankCanReturn() {
  val r=RecentLines();r.shouldSubmit("Go",0);r.observeVisibility("",100);r.observeVisibility("",1200)
  assertTrue(r.shouldSubmit("Go",1300))
 }
 @Test fun failedTranslationCanRetry() {
  val r=RecentLines();r.shouldSubmit("Go now",0);r.forget("Go now")
  assertTrue(r.shouldSubmit("Go now",500))
 }
 @Test fun dedupExpiresAndNewNumbersRemainDistinct() {
  val r=RecentLines();assertTrue(r.shouldSubmit("Score 100",0));assertTrue(r.shouldSubmit("Score 101",100))
  assertTrue(r.shouldSubmit("Score 100",8100))
 }
}
