package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SpeakerRouterTest {
 @Test fun namesKeepTheirSlotsAcrossReorderedDialogue() {
  val r=SpeakerRouter();val first=r.route("Jason: Go now\nLucia: Wait",0,true)
  val next=r.route("Lucia: Okay\nJason: Run",0,true)
  assertNotEquals(first[0].slot,first[1].slot)
  assertEquals(first[0].slot,next[1].slot);assertEquals(first[1].slot,next[0].slot)
 }
 @Test fun savedNamesRestoreTheirVoiceSlots() {
  val original=SpeakerRouter();original.route("Jason: Go\nLucia: Wait",0,true)
  val restored=SpeakerRouter();restored.restore(original.bindings())
  assertEquals(2,restored.route("Lucia: Okay",0,true).single().slot)
 }
 @Test fun unlabeledMicrophoneUsesManualSelection() {
  val r=SpeakerRouter();val entry=r.route("Hello there",3,false).single()
  assertEquals(3,entry.slot);assertEquals("Hello there",entry.text)
 }
 @Test fun subtitleNamesAreNotSentAsDialogue() {
  val r=SpeakerRouter();val e=r.route("[Lucia] Hello\nHow are you?",0,true).single()
  assertEquals("Lucia",e.name);assertEquals("Hello\nHow are you?",e.text)
 }
 @Test fun timeAndHudLabelsAreNotSpeakers() {
  val r=SpeakerRouter();assertEquals(0,r.route("12:34\nHP: 100",0,true).single().slot)
 }
 @Test fun moreThanEightNamesFallBackInsteadOfGrowingForever() {
  val r=SpeakerRouter(2);r.route("Alice: A\nBob: B",0,true)
  assertEquals(0,r.route("Charlie: C",0,true).single().slot)
 }
}
