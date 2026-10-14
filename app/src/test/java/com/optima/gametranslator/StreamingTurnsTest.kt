package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class StreamingTurnsTest {
 @Test fun tinyFragmentJoinsContinuationInsteadOfBeingSpokenTwice() {
  val t=StreamingTurns()
  assertFalse(t.update("I need",true,0).single().final)
  assertEquals("I need your",t.update("your",false,200).single().text)
  assertTrue(t.tick(800).isEmpty())
  assertEquals("I need your help right now",t.update("your help right now",true,1000).single().text)
  assertTrue(t.tick(2000).isEmpty())
 }
 @Test fun shortCompleteAnswerStillCommitsAfterGrace() {
  val t=StreamingTurns();t.update("yes",true,0)
  assertTrue(t.tick(649).isEmpty());assertEquals("yes",t.tick(650).single().text)
  assertTrue(t.tick(900).isEmpty())
 }
 @Test fun revisedPreviewNeverBecomesConfirmedText() {
  val t=StreamingTurns();t.update("we go",false,0);t.update("we went",false,400)
  assertTrue(t.tick(3000).isEmpty())
  assertEquals("we went to the station",t.update("we went to the station",true,4000).single().text)
 }
 @Test fun repeatedDialogueRemainsRepeated() {
  val t=StreamingTurns()
  assertEquals("watch out!",t.update("watch out!",true,0).single().text)
  assertEquals("watch out!",t.update("watch out!",true,1000).single().text)
 }
 @Test fun emptyEndpointCannotEraseAlreadyConfirmedFragment() {
  val t=StreamingTurns();t.update("hello",true,0)
  assertEquals("hello",t.update("",true,100).single().text)
 }
 @Test fun longFinalIsNotSplitAtAWordCounter() {
  val text=(1..50).joinToString(" ") { "word$it" }
  assertEquals(text,StreamingTurns().update(text,true,0).single().text)
 }
 @Test fun modePresetsAlwaysRequireSilenceForLongTurns() {
  for(m in 0..2) { val p=SpeechEndpointPolicy.forMode(m)
   assertTrue(p.longTurnSilence>0);assertTrue(p.longTurnSeconds>=18)
   assertTrue(p.trailingSilence>=.85f);assertTrue(p.paths>=2)
  }
 }
}
