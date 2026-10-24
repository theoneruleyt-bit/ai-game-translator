package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ListenRegressionTest {
 private val predictions by lazy {
  checkNotNull(javaClass.getResourceAsStream("/native-punctuation-0_23.tsv")).bufferedReader().useLines { rows ->
   rows.associate { row -> val c=row.split('\t');val d=Base64.getDecoder()
    String(d.decode(c[0]),Charsets.UTF_8) to String(d.decode(c[1]),Charsets.UTF_8) }
  }
 }
 private val uppercasePredictions by lazy {
  checkNotNull(javaClass.getResourceAsStream("/native-uppercase-punctuation-0_28.tsv")).bufferedReader().useLines { rows ->
   rows.associate { row -> val c=row.split('\t');val d=Base64.getDecoder()
    String(d.decode(c[0]),Charsets.UTF_8) to String(d.decode(c[1]),Charsets.UTF_8) }
  }
 }
 private fun replay(text: String,normalize: Boolean): Pair<List<String>,String> {
  val clauses=AdaptiveSpeechClauses();val consensus=TranscriptConsensus();val turns=StreamingTurns()
  val spoken=mutableListOf<String>();val words=text.uppercase(java.util.Locale.ROOT).split(' ');var now=0L
  val predict:(String)->String={ input -> if(normalize)checkNotNull(predictions[PunctuationInput.prepare(input)]) else checkNotNull(uppercasePredictions[input]) }
  for(n in 3..words.size step 3) {
   val current=words.take(n).joinToString(" ");val stable=consensus.observe(current,true)
   clauses.updateStable(current,now,0,false,stable,predict).forEach { output ->
    turns.update(output.text,output.final,now).filter { it.final }.forEach { spoken.add(it.text) }
   };now+=350
  }
  val before=spoken.toList()
  clauses.update(text.uppercase(java.util.Locale.ROOT),now+350,700,true,predict).forEach { output ->
   turns.update(output.text,output.final,now+350).filter { it.final }.forEach { spoken.add(it.text) }
  }
  turns.flushConfirmed().forEach { spoken.add(it.text) }
  return before to spoken.joinToString(" ").filter { it.isLetterOrDigit() }.lowercase(java.util.Locale.ROOT)
 }
 @Test fun uppercaseNativeShapeNoLongerWaitsForTheWholeGameDialogue() {
  val text="we need to find another way out the guards are coming down the stairs and we do not have much time i will meet you at the station bring the car around and wait by the gate do not leave without me"
  assertTrue(replay(text,false).first.isEmpty())
  val fixed=replay(text,true);assertTrue(fixed.first.isNotEmpty())
  assertEquals(text.filter { it.isLetterOrDigit() },fixed.second)
 }
 @Test fun uppercaseSingleSpeakerKeepsEveryWordAndCommitsBeforeEndpoint() {
  val text="we went to the station and the guard told us to wait but the train had already left i have been waiting for you for a long time and now we need to leave because the police are coming"
  val fixed=replay(text,true);assertTrue(fixed.first.isNotEmpty())
  assertEquals(text.filter { it.isLetterOrDigit() },fixed.second)
 }
 @Test fun EnglishPreparationIsIndependentOfTurkishSystemLocale() {
  val previous=java.util.Locale.getDefault()
  try { java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
   assertEquals("i will meet you at 5",PunctuationInput.prepare("I WILL MEET YOU AT 5"))
  } finally { java.util.Locale.setDefault(previous) }
 }
 @Test fun aRewrittenDraftStillAppearsButCannotCrossAnEndpointOrANewerDisplay() {
  // e.g. request "we went home" completes after recognition changes to "we went alone".
  assertTrue(PreviewDelivery.allows(true,4,3,false,false,"we went alone"))
  assertFalse(PreviewDelivery.allows(false,4,3,false,false,"we went alone"))
  assertFalse(PreviewDelivery.allows(true,4,5,false,false,"we went alone"))
  assertFalse(PreviewDelivery.allows(true,4,3,false,true,"we went alone"))
  assertFalse(PreviewDelivery.allows(true,4,3,false,false,""))
 }
 @Test fun confirmedShortTextCannotBeHeldForeverByUnconfirmedContinuation() {
  val t=StreamingTurns();t.update("I need",true,0);t.update("help",false,200)
  assertTrue(t.tick(800).isEmpty())
  assertEquals("I need",t.tick(1250).single().text)
  assertTrue(t.tick(2000).isEmpty())
  assertEquals("help with the car",t.update("help with the car",true,2500).single().text)
 }
 @Test fun traceIsOptInBoundedAndClearedWhenDisabled() {
  val t=ListeningTrace(2);t.record(0,"asr","private");assertTrue(t.snapshot().isEmpty())
  t.enabled=true;repeat(3) { t.record(it.toLong(),"asr","word$it") }
  assertEquals(listOf("word1","word2"),t.snapshot().map { it.text })
  t.enabled=false;assertTrue(t.snapshot().isEmpty())
 }
 @Test fun diagnosticSnapshotCannotChangeOrGrowTheLiveRing() {
  val t=ListeningTrace();t.enabled=true;t.record(0,"asr","x".repeat(1000))
  val snapshot=t.snapshot();assertEquals(500,snapshot.single().text.length)
  t.clear();assertEquals(1,snapshot.size);assertTrue(t.snapshot().isEmpty())
 }
}
