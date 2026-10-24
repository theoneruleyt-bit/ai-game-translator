package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SilentTurnIntegrationTest {
 private fun run(adaptive: Boolean): List<String> {
  val boundary=SpeechTurnBoundary();val clauses=AdaptiveSpeechClauses(adaptive);val turns=StreamingTurns()
  val spoken=mutableListOf<String>();val line="we need to leave now"
  fun publish(events: List<StreamingTurns.Output>) { spoken.addAll(events.filter { it.final }.map { it.text }) }
  for(t in 0L..20000L step 50L) {
   val speech=t<1000
   boundary.observe(speech,t)
   val text=boundary.accept(if(t<1800)line else "$line thank you thank you",t)
   if(boundary.mustEnd(t)) {
    clauses.update(text,t,t-950,true) { it }.forEach { publish(turns.update(it.text,it.final,t)) }
    boundary.reset()
   } else if(text.isNotBlank()) {
    clauses.update(text,t,if(speech)0 else t-950,false) { it }.forEach { publish(turns.update(it.text,it.final,t)) }
   }
   publish(turns.tick(t))
  }
  return spoken
 }
 @Test fun twentySecondsOfSilenceCannotReplayTheLastSoftCommittedSentence() {
  assertEquals(listOf("we need to leave now."),run(true))
 }
 @Test fun decoderWithoutEndpointStillClosesExactlyOnceWhenAdaptiveIsDisabled() {
  assertEquals(listOf("we need to leave now"),run(false))
 }
}
