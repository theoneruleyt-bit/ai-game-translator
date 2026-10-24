package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class TranscriptConsensusTest {
 @Test fun staleCallbacksCannotConfirmAFirstGuess() {
  val s=TranscriptConsensus();assertEquals(0,s.observe("we should leave now",true))
  repeat(30) { assertEquals(0,s.observe("we should leave now",false)) }
  assertEquals(4,s.observe("we should leave now",true))
 }
 @Test fun aRewrittenTailIsNotStable() {
  val s=TranscriptConsensus();s.observe("we should leave tomorrow",true)
  assertEquals(3,s.observe("we should leave tonight",true))
  assertEquals(4,s.observe("we should leave tonight quickly",true))
 }
 @Test fun revisionsInvalidateStabilityWithoutWaitingForNewAudio() {
  val s=TranscriptConsensus();s.observe("we should leave now",true);s.observe("we should leave now",true)
  assertEquals(2,s.observe("we should stay",false));assertEquals(0,s.observe("",false))
 }
 @Test fun caseAndPunctuationAreEquivalentButNewTurnsStartEmpty() {
  val s=TranscriptConsensus();s.observe("Come here!",true)
  assertEquals(2,s.observe("COME HERE",true));s.reset();assertEquals(0,s.observe("COME HERE",true))
 }
 @Test fun unconfirmedClauseCannotReachSpeechEvenAfterTimerExpires() {
  val clauses=AdaptiveSpeechClauses();val text="we need to leave now the guards are coming"
  val punctuate:(String)->String={ it.replace("now the","now. The") }
  clauses.updateStable(text,0,0,false,0,punctuate)
  assertFalse(clauses.updateStable(text,500,0,false,4,punctuate).any { it.final })
  assertEquals("we need to leave now.",clauses.updateStable(text,600,0,false,5,punctuate).first { it.final }.text)
  assertEquals("the guards are coming",clauses.updateStable(text,1000,700,true,0) { it }.single().text)
 }
 @Test fun endpointStillFlushesAnUnconfirmedShortReply() {
  assertEquals("yes",AdaptiveSpeechClauses().updateStable("yes",0,700,true,0) { it }.single().text)
 }
}
