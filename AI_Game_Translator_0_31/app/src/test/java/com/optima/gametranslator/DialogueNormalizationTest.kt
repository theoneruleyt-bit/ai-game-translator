package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class DialogueNormalizationTest {
 private val pack=ConversationLexicon(mapOf("Don't move" to "Kıpırdama.","Cannot get out" to "Çıkamıyorum.","I'm trying to help" to "Yardım etmeye çalışıyorum."))
 @Test fun asrSpellingAndNegationMatchTheSameWholeExpression() {
  assertEquals("Kıpırdama.",pack.exactSentences("dont move"))
  assertEquals("Kıpırdama.",pack.exactSentences("do not move"))
  assertEquals("Çıkamıyorum.",pack.exactSentences("can't get out"))
  assertNull(pack.exactSentences("move"));assertNull(pack.exactSentences("don't move the chair"))
 }
 @Test fun colloquialReductionDoesNotInventASubject() {
  assertEquals("I'm trying to help",pack.normalizeDialogueEnglish("I'm tryna help"))
  assertEquals("Yardım etmeye çalışıyorum.",pack.exactSentences("I'm tryna help"))
  assertEquals("he ain't here",pack.normalizeDialogueEnglish("he ain't here"))
 }
 @Test fun ambiguousAliasDoesNotPickAnArbitraryTranslation() {
  val a=ConversationLexicon(mapOf("do not move" to "A","don't move" to "B"));assertNull(a.exactSentences("dont move"))
 }
 @Test fun cameraLookupKeepsItsBehavior() {
  assertNull(pack.exact("dont move",false));assertEquals("dont move",pack.normalizeEnglish("dont move"))
 }
}
