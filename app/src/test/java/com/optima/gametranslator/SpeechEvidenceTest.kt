package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SpeechEvidenceTest {
 @Test fun recognizerWordsWithoutSpeechNeverPass() {
  val e=SpeechEvidence();repeat(100) { e.observe(false);assertFalse(e.allowsPreview()) }
  assertFalse(e.endTurn())
 }
 @Test fun realSpeechIsKeptThroughTrailingSilenceButNotNextSilentTurn() {
  val e=SpeechEvidence();e.observe(true);repeat(10) { e.observe(false) }
  assertTrue(e.allowsPreview());assertTrue(e.endTurn());assertFalse(e.allowsPreview());assertFalse(e.endTurn())
 }
 @Test fun microphoneReadsDoNotChangeVadWindowContents() {
  val w=PcmWindows();val output=mutableListOf<FloatArray>()
  w.accept(FloatArray(800) { it.toFloat() }) { output.add(it) }
  w.accept(FloatArray(800) { (it+800).toFloat() }) { output.add(it) }
  assertEquals(3,output.size)
  assertArrayEquals(FloatArray(1536) { it.toFloat() },output.flatMap { it.toList() }.toFloatArray(),0f)
  w.accept(FloatArray(448) { (it+1600).toFloat() }) { output.add(it) }
  assertEquals(4,output.size);assertEquals(2047f,output.last().last(),0f)
 }
}
