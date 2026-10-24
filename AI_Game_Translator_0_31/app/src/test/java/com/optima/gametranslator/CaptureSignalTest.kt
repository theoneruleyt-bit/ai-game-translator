package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class CaptureSignalTest {
 @Test fun silenceIsMeasuredWithoutInventingSpeechConfidence() {
  val s=CaptureSignal.inspect(FloatArray(800),-5);assertEquals(0.0,s.rms,0.0001)
  assertEquals(0.0,s.clippedPortion,0.0001);assertEquals(0L,s.backlogMillis)
 }
 @Test fun clippingIsDetectedSeparatelyFromLoudness() {
  val s=CaptureSignal.inspect(floatArrayOf(1f,-1f,.2f,-.2f),900)
  assertEquals(.5,s.clippedPortion,.0001);assertEquals(900L,s.backlogMillis)
 }
}
