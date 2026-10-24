package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class RequestBudgetTest {
 @Test fun stalledPreviewCannotBlockConfirmedTranslation() {
  val b=RequestBudget();assertNotNull(b.acquire(RequestBudget.Lane.PREVIEW))
  assertNull(b.acquire(RequestBudget.Lane.PREVIEW));assertNotNull(b.acquire(RequestBudget.Lane.FINAL))
 }
 @Test fun nativeRequestsStayBoundedEvenAfterUiStopsWaiting() {
  val b=RequestBudget();val a=b.acquire(RequestBudget.Lane.FINAL)!!;val c=b.acquire(RequestBudget.Lane.FINAL)!!
  assertNull(b.acquire(RequestBudget.Lane.FINAL));b.release(a)
  assertNotNull(b.acquire(RequestBudget.Lane.FINAL));assertEquals(2,b.count(RequestBudget.Lane.FINAL))
  b.release(c);assertEquals(1,b.count(RequestBudget.Lane.FINAL))
 }
 @Test fun queuedFinalsCannotFreezeTheIndependentPreviewLane() {
  val b=RequestBudget();assertNotNull(b.acquire(RequestBudget.Lane.FINAL));assertNotNull(b.acquire(RequestBudget.Lane.FINAL))
  assertNotNull(b.acquire(RequestBudget.Lane.PREVIEW));assertNull(b.acquire(RequestBudget.Lane.PREVIEW))
  assertEquals(2,b.count(RequestBudget.Lane.FINAL));assertEquals(1,b.count(RequestBudget.Lane.PREVIEW))
 }
 @Test fun lateDuplicateCompletionCannotReleaseAnotherRequest() {
  val b=RequestBudget();val a=b.acquire(RequestBudget.Lane.PREVIEW)!!;b.release(a)
  val next=b.acquire(RequestBudget.Lane.PREVIEW)!!;b.release(a)
  assertNull(b.acquire(RequestBudget.Lane.PREVIEW));b.release(next)
  assertNotNull(b.acquire(RequestBudget.Lane.PREVIEW))
 }
}
