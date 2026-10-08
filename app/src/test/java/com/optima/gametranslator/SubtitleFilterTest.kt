package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class SubtitleFilterTest {
 @Test fun clocksAndProgressAreIgnored() {
  val f=SubtitleFilter();assertFalse(f.keep("12:34",.5f,.8f,0));assertFalse(f.keep("01:23 / 45:00",.5f,.8f,1))
 }
 @Test fun hudCountersAreIgnoredButSpokenNumbersStay() {
  val f=SubtitleFilter();assertFalse(f.keep("HP 100",.1f,.1f,0));assertFalse(f.keep("FPS 60",.1f,.1f,0))
  assertTrue(f.keep("Collect 100 coins",.5f,.6f,0))
 }
 @Test fun stationaryNameStopsContributingAfterEightSeconds() {
  val f=SubtitleFilter();assertTrue(f.keep("Channel name",.1f,.1f,0))
  for(t in 1000L..7000L step 1000)assertTrue(f.keep("Channel name",.1f,.1f,t))
  assertFalse(f.keep("Channel name",.1f,.1f,8000))
  assertTrue(f.keep("A different subtitle",.5f,.6f,8100))
 }
 @Test fun textCanReturnAfterLeavingTheFrame() {
  val f=SubtitleFilter();f.keep("Name",.5f,.5f,0)
  assertTrue(f.keep("Name",.5f,.5f,10000))
 }
}
