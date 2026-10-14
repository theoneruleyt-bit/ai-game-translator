package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class PlaybackDeadlineTest {
 @Test fun acceptedButNeverStartedExpires() {
  val d=PlaybackDeadline();d.begin("a",100,1000)
  assertFalse(d.expired("a",12099));assertTrue(d.expired("a",12100))
 }
 @Test fun realPlaybackGetsLongerThanStartupWindow() {
  val d=PlaybackDeadline();d.begin("a",0,20000);d.started("a",1000)
  assertFalse(d.expired("a",12000));assertFalse(d.expired("a",95999));assertTrue(d.expired("a",96000))
 }
 @Test fun oldCallbackCannotExtendNewJob() {
  val d=PlaybackDeadline();d.begin("a",0,1000);d.begin("b",100,1000);d.started("a",11000)
  assertFalse(d.expired("a",999999));assertTrue(d.expired("b",12100))
 }
 @Test fun stoppedJobNoLongerExpires() {
  val d=PlaybackDeadline();d.begin("a",0,1000);d.clear();assertFalse(d.expired("a",99999))
 }
}
