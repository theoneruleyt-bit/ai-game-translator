package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
class EnglishUnigramTest {
 private fun lines(name: String)=checkNotNull(javaClass.getResourceAsStream(name)).bufferedReader().use { it.readLines() }
 private val tokenizer by lazy { EnglishUnigram(lines("/mt-pieces.tsv")) }
 @Test fun nativeSentencePieceParityFor684EnglishDialogueAndNegationInputs() {
  val fixtures=lines("/english-unigram-fixtures.tsv");assertEquals(684,fixtures.size)
  for(row in fixtures) {
   val parts=row.split('\t');val text=String(Base64.getDecoder().decode(parts[0]),Charsets.UTF_8)
   val expected=parts[1].split(',').map(String::toInt).toIntArray()
   assertArrayEquals(text,expected,tokenizer.encode(text))
  }
 }
 @Test fun nativeTurkishDetokenizationPreservesLettersAndNegation() {
  for(row in lines("/turkish-decode-fixtures.tsv")) {
   val parts=row.split('\t');val text=String(Base64.getDecoder().decode(parts[0]),Charsets.UTF_8)
   assertEquals(text,tokenizer.decode(parts[1].split(',').map(String::toInt)))
  }
 }
 @Test fun unsupportedUnicodeFailsExplicitlyRatherThanUsingApproximateNormalization() {
  assertThrows(IllegalArgumentException::class.java) { tokenizer.encode("Çağrı is here") }
  assertThrows(IllegalArgumentException::class.java) { tokenizer.encode("hello 😀") }
 }
 @Test fun unknownGeneratedTokenDoesNotBecomeASpokenGuess() {
  assertThrows(IllegalArgumentException::class.java) { tokenizer.decode(listOf(1,0)) }
 }
 @Test fun largeInputIsNotSilentlyTruncatedByTheTokenizer() {
  val ids=tokenizer.encode((1..400).joinToString(" ") { "guard" })
  assertTrue(ids.size>256);assertEquals(0,ids.last())
 }
}
