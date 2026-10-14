package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class PartialStabilityTest {
 @Test fun changedWordsRestartStabilityWindow() {
  val p=PartialStability();p.observe("go home",0)
  assertFalse(p.ready("go home",249));assertTrue(p.ready("go home",250))
  p.observe("go now",300);assertFalse(p.ready("go now",500));assertTrue(p.ready("go now",550))
 }
 @Test fun samePreviewDoesNotRestartWindow() {
  val p=PartialStability();p.observe("hello",0);p.observe("hello",200)
  assertTrue(p.ready("hello",250));p.reset();assertFalse(p.ready("hello",9999))
 }
 @Test fun continuouslyChangingSpeechStillGetsAPreview() {
  val p=PartialStability()
  for(t in listOf(0L,200L,400L,600L,800L))p.observe("words $t",t)
  assertFalse(p.ready("words 800",850));assertTrue(p.ready("words 800",900))
  p.submitted(900);p.observe("words 1000",1000)
  assertFalse(p.ready("words 1000",1100))
 }
}
