package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class LineQueueTest {
 @Test fun subtitlesKeepTheirOrder() {
  val q=LineQueue();q.offer("First",0);q.offer("Second",100);q.offer("Third",200)
  assertEquals("First",q.take()?.text);assertEquals("Second",q.take()?.text)
  assertEquals("Third",q.take()?.text);assertNull(q.take())
 }
 @Test fun slowTranslationDoesNotExpireAcceptedLines() {
  val q=LineQueue();q.offer("First",0);q.offer("Second",100000)
  assertEquals("First",q.take()?.text)
 }
 @Test fun overflowIsReportedAndDoesNotReplaceEarlierLines() {
  val q=LineQueue(2);assertTrue(q.offer("First",0));assertTrue(q.offer("Second",1))
  assertFalse(q.offer("Third",2));assertEquals("First",q.take()?.text)
  assertTrue(q.offer("Third",3));assertEquals("Second",q.take()?.text);assertEquals("Third",q.take()?.text)
 }
 @Test fun stopClearsAllPendingLines() {
  val q=LineQueue();q.offer("First",0);q.clear();assertEquals(0,q.size);assertNull(q.take())
 }
}
