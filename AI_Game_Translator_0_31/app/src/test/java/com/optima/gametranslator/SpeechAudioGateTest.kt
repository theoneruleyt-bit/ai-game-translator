package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SpeechAudioGateTest {
 @Test fun idleAudioIsNotDecodedAndLeadInIsPreserved() {
  val g=SpeechAudioGate(8)
  assertTrue(g.accept(floatArrayOf(1f,2f,3f,4f),false).isEmpty())
  assertTrue(g.accept(floatArrayOf(5f,6f,7f,8f),false).isEmpty())
  val frames=g.accept(floatArrayOf(9f,10f,11f,12f),true)
  assertArrayEquals(floatArrayOf(5f,6f,7f,8f,9f,10f,11f,12f),frames.flatMap { it.toList() }.toFloatArray(),0f)
 }
 @Test fun trailingSilenceIsNotDroppedAndLeadInIsNotReplayedTwice() {
  val g=SpeechAudioGate();g.accept(floatArrayOf(1f),true)
  assertArrayEquals(floatArrayOf(2f,3f),g.accept(floatArrayOf(2f,3f),false).single(),0f)
  g.endTurn();assertTrue(g.accept(floatArrayOf(4f),false).isEmpty())
 }
 @Test fun aLongIdleFrameStillHasBoundedPrefixMemory() {
  val g=SpeechAudioGate(3);assertTrue(g.accept(floatArrayOf(1f,2f,3f,4f,5f),false).isEmpty())
  assertArrayEquals(floatArrayOf(4f,5f,6f),g.accept(floatArrayOf(6f),true).flatMap { it.toList() }.toFloatArray(),0f)
 }
}
