package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class LatestLineTest {
 @Test fun latestWaitingSubtitleReplacesOlderOne() {
  val q=LatestLine();q.offer("First",0);q.offer("Next",200)
  assertEquals("Next",q.take(300)?.text);assertNull(q.take(400))
 }
 @Test fun oldWaitingSubtitleExpires() {
  val q=LatestLine();q.offer("Old",0);assertNull(q.take(3001))
 }
 @Test fun stoppingClearsPendingSpeech() {
  val q=LatestLine();q.offer("Next",0);q.clear();assertNull(q.take(100))
 }
}
