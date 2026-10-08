package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class StabilityGateTest {
 @Test fun shortSubtitleCanPassBeforeOld600msWindow() {
  val g=StabilityGate();assertNull(g.observe("Run!",0))
  assertNull(g.observe("Run!",100));assertEquals("Run!",g.observe("Run!",150))
 }
 @Test fun oneFrameNoiseDoesNotPass() {
  val g=StabilityGate();assertNull(g.observe("Hello",0));assertNull(g.observe("Help",160))
  assertEquals("Help",g.observe("Help",320))
 }
 @Test fun punctuationJitterDoesNotRestartStability() {
  val g=StabilityGate();g.observe("Go now!",0)
  assertEquals("Go now.",g.observe("Go now.",160));g.commit("Go now.")
  assertNull(g.observe("Go now!",320))
 }
 @Test fun minorLongLineOcrJitterIsTolerated() {
  val g=StabilityGate();g.observe("We need to leave this city tonight",0)
  assertEquals("We need to leave this clty tonight",g.observe("We need to leave this clty tonight",160))
 }
 @Test fun numberChangesAreDistinct() {
  val g=StabilityGate();g.observe("Collect exactly 100 coins for this mission",0)
  assertNull(g.observe("Collect exactly 101 coins for this mission",160))
  assertEquals("Collect exactly 101 coins for this mission",g.observe("Collect exactly 101 coins for this mission",320))
 }
 @Test fun shortDistinctWordsStayDistinct() {
  val g=StabilityGate();g.observe("Wait",0);g.commit("Wait");g.observe("Want",160)
  assertEquals("Want",g.observe("Want",320))
 }
 @Test fun duplicateAndShortGapAreSuppressed() {
  val g=StabilityGate();g.observe("Go!",0);g.commit("Go!")
  assertNull(g.observe("Go!",2000));g.observe("",2100);g.observe("Go!",2500)
  assertNull(g.observe("Go!",2700))
 }
 @Test fun realRepetitionAfterAbsence() {
  val g=StabilityGate();g.observe("Go",0);g.commit("Go");g.observe("",1000);g.observe("",2100)
  g.observe("Go",2200);assertEquals("Go",g.observe("Go",2400))
 }
 @Test fun newLineDoesNotNeedBlankFrame() {
  val g=StabilityGate();g.observe("First",0);g.commit("First");g.observe("Next",200)
  assertEquals("Next",g.observe("Next",400))
 }
 @Test fun failedTranslationCanBeRetried() {
  val g=StabilityGate();g.observe("Go",0);g.commit("Go");g.release("Go")
  assertEquals("Go",g.observe("Go",200))
 }
}
