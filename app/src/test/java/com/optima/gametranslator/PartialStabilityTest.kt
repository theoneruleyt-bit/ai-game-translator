package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class PartialStabilityTest {
 @Test fun changedWordsRestartStabilityWindow() {
  val p=PartialStability();p.observe("go home",0)
  assertFalse(p.ready("go home",179));assertTrue(p.ready("go home",180))
  p.observe("go now",100);assertFalse(p.ready("go now",279));assertTrue(p.ready("go now",280))
 }
 @Test fun samePreviewDoesNotRestartWindow() {
  val p=PartialStability();p.observe("hello",0);p.observe("hello",150)
  assertTrue(p.ready("hello",180));p.reset();assertFalse(p.ready("hello",9999))
 }
 @Test fun continuouslyChangingSpeechGetsAnEarlyBoundedPreview() {
  val p=PartialStability()
  for(t in listOf(0L,100L,200L,300L,400L))p.observe("words $t",t)
  assertFalse(p.ready("words 400",449));assertTrue(p.ready("words 400",450))
  p.submitted(450);p.observe("words 500",500)
  assertFalse(p.ready("words 500",799));assertTrue(p.ready("words 500",800))
 }
 @Test fun resetDoesNotThrottleANewSession() {
  val p=PartialStability();p.observe("hello",0);p.submitted(200);p.reset()
  p.observe("new session",250);assertTrue(p.ready("new session",430))
 }
 @Test fun staleTextCannotTriggerAnotherPreview() {
  val p=PartialStability();p.observe("changed words",0)
  assertFalse(p.ready("old words",1000))
 }
}
