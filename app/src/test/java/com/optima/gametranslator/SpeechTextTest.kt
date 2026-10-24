package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SpeechTextTest {
 @Test fun longTextPreservesEveryWordAndOrder() {
  val text=(1..120).joinToString(" ") { "kelime$it" };val chunks=SpeechText.split(text,80)
  assertEquals(text,chunks.joinToString(" "));assertTrue(chunks.all { it.length<=80 })
 }
 @Test fun emojiAtChunkBoundaryIsNotCorrupted() {
  val text="a".repeat(79)+"🚗"+"b".repeat(90);val chunks=SpeechText.split(text,80)
  assertEquals(text,chunks.joinToString(""))
  for(chunk in chunks) { assertFalse(Character.isHighSurrogate(chunk.last()));assertFalse(Character.isLowSurrogate(chunk.first())) }
 }
 @Test fun sentenceBoundaryWinsOverLaterSpace() {
  val chunks=SpeechText.split("Merhaba arkadaşım. Burada biraz daha uzun bir yazı var.",30)
  assertEquals("Merhaba arkadaşım.",chunks.first())
 }
 @Test fun noWhitespaceTextRemainsBoundedAndComplete() {
  val text="a".repeat(901);val chunks=SpeechText.split(text,260)
  assertEquals(text,chunks.joinToString(""));assertTrue(chunks.all { it.length<=260 })
 }
}
