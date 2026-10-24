package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class AdaptiveSpeechClausesTest {
 private fun finals(events: List<AdaptiveSpeechClauses.Output>)=events.filter { it.final }.map { it.text }
 private val dialogue="we need to find another way out the guards are coming down the stairs and we do not have much time"
 private val punctuated="We need to find another way out. The guards are coming down the stairs, and we do not have much time"
 @Test fun continuousSpeakerCanBeTranslatedBeforeAnyNativeEndpoint() {
  val s=AdaptiveSpeechClauses();assertTrue(finals(s.update(dialogue,0,0,false) { punctuated }).isEmpty())
  val events=s.update(dialogue,300,0,false) { punctuated }
  assertEquals(listOf("We need to find another way out."),finals(events))
  assertTrue(events.last().text.startsWith("the guards"));assertFalse(events.last().final)
 }
 @Test fun rapidlyGrowingAsrTextDoesNotStarveAStableInternalBoundary() {
  val s=AdaptiveSpeechClauses()
  val base="we need to leave now the guards"
  val format:(String)->String={ it.replace("now the","now. The") }
  s.update(base,0,0,false,format)
  assertTrue(finals(s.update("$base are",150,0,false,format)).isEmpty())
  assertEquals(listOf("we need to leave now."),finals(s.update("$base are coming",300,0,false,format)))
 }
 @Test fun nativeFinalNeverReplaysAnAlreadySpokenPrefix() {
  val s=AdaptiveSpeechClauses();s.update(dialogue,0,0,false) { punctuated };s.update(dialogue,300,0,false) { punctuated }
  val final=s.update(dialogue,1000,700,true) { it }.single()
  assertTrue(final.text.startsWith("the guards"));assertFalse(final.text.contains("another way out"))
 }
 @Test fun realRepeatedDialogueInNewNativeTurnIsPreserved() {
  val s=AdaptiveSpeechClauses()
  repeat(2) { assertEquals("we need to leave now",s.update("we need to leave now",it*1000L,700,true) { t->t }.single().text) }
 }
 @Test fun trailingModelPeriodAloneCannotCommitWhileSpeakerContinues() {
  val s=AdaptiveSpeechClauses();val text="we need to leave now"
  s.update(text,0,0,false) { "We need to leave now." }
  assertTrue(finals(s.update(text,1000,0,false) { "We need to leave now." }).isEmpty())
 }
 @Test fun oneLookAheadWordIsNotEnoughToSpeakAnInternalBoundary() {
  val s=AdaptiveSpeechClauses();val text="we need to leave now the"
  s.update(text,0,0,false) { "We need to leave now. The" }
  assertTrue(finals(s.update(text,1000,0,false) { "We need to leave now. The" }).isEmpty())
 }
 @Test fun shortPauseCanCommitAStablePredicateWithoutEngineEndpoint() {
  val s=AdaptiveSpeechClauses();val text="we need to get out of here"
  s.update(text,0,0,false) { it }
  assertTrue(finals(s.update(text,300,100,false) { it }).isEmpty())
  assertEquals(listOf("we need to get out of here."),finals(s.update(text,350,150,false) { it }))
  assertEquals("",s.update(text,1000,700,true) { it }.single().text)
 }
 @Test fun revisedWordsBeforeCommitRestartTheCandidateWindow() {
  val s=AdaptiveSpeechClauses();s.update("we need to leave now",0,0,false) { it }
  val revised="we need to leave tomorrow"
  assertTrue(finals(s.update(revised,300,150,false) { it }).isEmpty())
  assertEquals(listOf("we need to leave tomorrow."),finals(s.update(revised,650,300,false) { it }))
 }
 @Test fun incompleteDependentClauseWaitsForItsMainClause() {
  val s=AdaptiveSpeechClauses();val text="when we get there"
  s.update(text,0,0,false) { "When we get there," }
  assertTrue(finals(s.update(text,1000,300,false) { "When we get there," }).isEmpty())
 }
 @Test fun danglingFunctionWordCannotBecomeASoftFinal() {
  val s=AdaptiveSpeechClauses();val text="i wanted to tell you that"
  s.update(text,0,0,false) { "I wanted to tell you that." }
  assertTrue(finals(s.update(text,1000,300,false) { "I wanted to tell you that." }).isEmpty())
 }
 @Test fun commaBetweenMeaningGroupsCanCommitWithLookAhead() {
  val s=AdaptiveSpeechClauses();val raw="i will meet you at the station bring the car around and wait by the gate"
  val formatted="I will meet you at the station, bring the car around and wait by the gate"
  s.update(raw,0,0,false) { formatted }
  assertEquals(listOf("I will meet you at the station,"),finals(s.update(raw,350,0,false) { formatted }))
 }
 @Test fun prefixInsertionAlignsToTheStillUnspokenRightContext() {
  val s=AdaptiveSpeechClauses();val raw="we need to find another way out the guards are coming"
  val p="We need to find another way out. The guards are coming"
  s.update(raw,0,0,false) { p };s.update(raw,350,0,false) { p }
  val revised="we really need to find another way out the guards are coming downstairs"
  assertEquals("the guards are coming downstairs",s.update(revised,1000,700,true) { it }.single().text)
 }
 @Test fun prefixDeletionAlsoKeepsTheUnspokenContinuation() {
  val s=AdaptiveSpeechClauses();val raw="we need to find another way out the guards are coming"
  val p="We need to find another way out. The guards are coming"
  s.update(raw,0,0,false) { p };s.update(raw,350,0,false) { p }
  val revised="we need to find a way out the guards are coming downstairs"
  assertEquals("the guards are coming downstairs",s.update(revised,1000,700,true) { it }.single().text)
 }
 @Test fun inventedPunctuationModelWordsAreRejected() {
  val s=AdaptiveSpeechClauses();val text="we need to leave now"
  s.update(text,0,0,false) { "We steal the car. Shoot everyone now." }
  assertTrue(finals(s.update(text,500,0,false) { "We steal the car. Shoot everyone now." }).isEmpty())
  assertEquals(text,s.update(text,1000,700,true) { "invented words" }.single().text)
 }
 @Test fun decimalDotIsNotAWordBoundary() {
  val s=AdaptiveSpeechClauses();val text="the distance is 3.5 miles from here to the station"
  s.update(text,0,0,false) { "The distance is 3.5 miles from here to the station" }
  assertTrue(finals(s.update(text,500,0,false) { "The distance is 3.5 miles from here to the station" }).isEmpty())
 }
 @Test fun titleAbbreviationDoesNotCutThePersonName() {
  val s=AdaptiveSpeechClauses();val text="we need to speak to dr smith before we leave"
  s.update(text,0,0,false) { "We need to speak to Dr. Smith before we leave" }
  assertTrue(finals(s.update(text,500,0,false) { "We need to speak to Dr. Smith before we leave" }).isEmpty())
 }
 @Test fun disabledModeWaitsForTheNativeEndpointAndDoesNotLoadModel() {
  val s=AdaptiveSpeechClauses(false);var calls=0;val predict:(String)->String={ calls++;it }
  s.update(dialogue,0,0,false,predict);assertTrue(finals(s.update(dialogue,1000,1000,false,predict)).isEmpty())
  assertEquals(dialogue,s.update(dialogue,2000,1000,true,predict).single().text);assertEquals(0,calls)
 }
 @Test fun silentStableTextWithNoPredicateDoesNotBecomeAnEarlyClause() {
  val s=AdaptiveSpeechClauses();val text="the man in the street"
  s.update(text,0,0,false) { it };assertTrue(finals(s.update(text,1000,300,false) { it }).isEmpty())
 }
 @Test fun noBoundaryMeansNoWordCountCutEvenForALongMonologue() {
  val s=AdaptiveSpeechClauses();val text=(1..150).joinToString(" ") { "word$it" }
  s.update(text,0,0,false) { it };assertTrue(finals(s.update(text,5000,0,false) { it }).isEmpty())
  assertEquals(text,s.update(text,6000,700,true) { it }.single().text)
 }
 @Test fun resetCannotCarryAPreviousSpokenCursorIntoNewAudio() {
  val s=AdaptiveSpeechClauses();s.update(dialogue,0,0,false) { punctuated };s.update(dialogue,350,0,false) { punctuated };s.reset()
  assertEquals(dialogue,s.update(dialogue,1000,700,true) { it }.single().text)
 }
 @Test fun unanchoredRewriteDoesNotReplayAnAlreadySpokenTail() {
  val s=AdaptiveSpeechClauses();val t="we need to find another way out"
  s.update(t,0,0,false) { it };s.update(t,350,150,false) { it }
  assertEquals("",s.update("we need to find out a way",1000,700,true) { it }.single().text)
 }
 @Test fun uniqueSpokenSuffixPreservesNewContinuationAfterDeletion() {
  val s=AdaptiveSpeechClauses();val t="we need to find another way out"
  s.update(t,0,0,false) { it };s.update(t,350,150,false) { it }
  assertEquals("the door",s.update("we need another way out the door",1000,700,true) { it }.single().text)
 }
 @Test fun newWordsSurviveAnUnanchoredPrefixRewrite() {
  val s=AdaptiveSpeechClauses();val t="we need to leave now"
  s.update(t,0,0,false) { it };s.update(t,350,150,false) { it }
  assertEquals("the guards arrived",s.update("we have to leave now the guards arrived",1000,700,true) { it }.single().text)
 }
}
