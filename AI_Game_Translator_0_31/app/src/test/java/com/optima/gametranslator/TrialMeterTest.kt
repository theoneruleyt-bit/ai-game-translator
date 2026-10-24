package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class TrialMeterTest {
 @Test fun stoppedAndSetupTimeDoNotCount() {
  val t=TrialMeter();t.tick(90000);assertEquals(0L,t.usedMillis)
  t.start(100000);t.tick(101000);t.stop(102000);t.tick(500000)
  assertEquals(2000L,t.usedMillis)
 }
 @Test fun switchingSessionsSharesOneLimit() {
  val t=TrialMeter();t.start(0);t.stop(60000);t.start(90000);t.stop(150000)
  assertEquals(60000L,t.remainingMillis)
 }
 @Test fun expiryCannotOverflowOrRestart() {
  val t=TrialMeter(179000);t.start(0);t.tick(2000)
  assertEquals(0L,t.remainingMillis);assertFalse(t.running)
  t.start(5000);assertFalse(t.running)
 }
 @Test fun loadedConsumptionIsPreserved() {
  assertEquals(100000L,TrialMeter(80000).remainingMillis)
  assertEquals(0L,TrialMeter(Long.MAX_VALUE).remainingMillis)
 }
 @Test fun serverReconcileCannotResetLocalUsage() {
  val t=TrialMeter(50000);t.reconcile(10000);assertEquals(50000L,t.usedMillis)
  t.reconcile(100000);assertEquals(80000L,t.remainingMillis)
  t.start(0);t.reconcile(180000);assertFalse(t.running)
 }
 @Test fun backwardsClockCannotDoubleCount() {
  val t=TrialMeter();t.start(1000);t.tick(500);t.tick(1500)
  assertEquals(500L,t.usedMillis)
 }
}
