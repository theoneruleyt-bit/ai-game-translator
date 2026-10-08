package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class StabilityGateTest {
 @Test fun transientOcrDoesNotSpeak() {
  val g=StabilityGate(); assertNull(g.observe("He",0)); assertNull(g.observe("Hello",400))
  assertNull(g.observe("Hello",800)); assertEquals("Hello",g.observe("Hello",1000))
 }
 @Test fun duplicateAndShortGapAreSuppressed() {
  val g=StabilityGate();g.observe("Go!",0);g.observe("Go!",700);g.commit("Go!")
  assertNull(g.observe("Go!",2000));g.observe("",2100);g.observe("Go!",2500)
  assertNull(g.observe("Go!",3200))
 }
 @Test fun realRepetitionAfterAbsence() {
  val g=StabilityGate();g.observe("Go",0);g.commit("Go");g.observe("",1000);g.observe("",2600)
  g.observe("Go",2700);assertEquals("Go",g.observe("Go",3400))
 }
 @Test fun newLineDoesNotNeedBlankFrame() {
  val g=StabilityGate();g.observe("First",0);g.commit("First");g.observe("Next",700)
  assertEquals("Next",g.observe("Next",1400))
 }
}
