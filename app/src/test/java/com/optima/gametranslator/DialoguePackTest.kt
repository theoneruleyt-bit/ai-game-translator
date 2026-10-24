package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class DialoguePackTest {
 private val rows by lazy { checkNotNull(javaClass.getResourceAsStream("/dialogue-pack-v2.tsv")).bufferedReader().use { it.readLines() }.filter { it.isNotBlank()&&!it.startsWith("#") } }
 private val pack by lazy { ConversationLexicon(rows.associate { val p=it.split('\t');p[0] to p[1] }) }
 @Test fun completePackHas517UniqueNonemptyEntries() {
  assertEquals(517,rows.size);assertEquals(rows.size,rows.map { ConversationLexicon.key(it.substringBefore('\t')) }.toSet().size)
  assertTrue(rows.all { it.split('\t').size==2&&it.substringAfter('\t').isNotBlank() })
 }
 @Test fun idiomsDoNotBecomeLiteralBodyPartsOrTemperature() {
  assertEquals("Benimle dalga geçiyorsun.",pack.exactSentences("You’re pulling my leg!"))
  assertEquals("Ortalık yatışana kadar göze batmamamız gerekiyor.",pack.exactSentences("We need to lay low until the heat dies down."))
  assertEquals("Burası polis kaynıyor.",pack.exactSentences("This place is crawling with cops."))
 }
 @Test fun unknownContextAndNegationCannotBeRewrittenAsAKnownPositivePhrase() {
  assertNull(pack.exactSentences("You are not pulling my leg."))
  assertNull(pack.exactSentences("He is not buying a car."))
  assertNull(pack.exactSentences("The heat is 42 degrees."))
  assertNull(pack.exactSentences("I'm down."));assertNull(pack.exactSentences("That's sick."))
 }
 @Test fun profanityAndColloquialNegationAreRetainedInAuthoredExamples() {
  assertEquals("Siktir git.",pack.exactSentences("Fuck off!"))
  assertEquals("Hiç param yok.",pack.exactSentences("I ain't got no money."))
  assertEquals("Bu benim hatam değil.",pack.exactSentences("That's not on me."))
 }
 @Test fun questionIntonationCannotBeTurnedIntoAnAuthoredAssertion() {
  assertNull(pack.exactSentences("You're pulling my leg?"))
  assertEquals("Benimle dalga mı geçiyorsun?",pack.exactSentences("Are you pulling my leg?"))
 }
 @Test fun multipleShortMatchesKeepPausesBetweenSpokenSentences() {
  assertEquals("Ne haber? Siper al.",pack.exactSentences("What's up? Take cover."))
 }
 @Test fun MultipleSentencesMatchOnlyWhenEverySentenceIsKnown() {
  assertEquals("Benimle dalga geçiyorsun. Burası polis kaynıyor.",pack.exactSentences("You're pulling my leg. This place is crawling with cops."))
  assertNull(pack.exactSentences("You're pulling my leg. The train leaves at 18:42."))
 }
}
