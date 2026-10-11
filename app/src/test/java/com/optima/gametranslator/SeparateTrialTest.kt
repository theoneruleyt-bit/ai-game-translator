package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class SeparateTrialTest {
    @Test fun exhaustingCameraDoesNotExhaustListening() {
        val camera=TrialMeter();val listening=TrialMeter()
        camera.start(100);camera.stop(180100)
        assertEquals(0L,camera.remainingMillis)
        assertEquals(180000L,listening.remainingMillis)
        listening.start(180100);listening.stop(181100)
        assertEquals(179000L,listening.remainingMillis)
    }
    @Test fun modeSwitchAndReloadPreserveSeparateBalances() {
        val camera=TrialMeter();val listening=TrialMeter()
        camera.start(0);camera.stop(30000)
        listening.start(30000);listening.stop(40000)
        camera.start(50000);camera.stop(55000)
        assertEquals(145000L,TrialMeter(camera.usedMillis).remainingMillis)
        assertEquals(170000L,TrialMeter(listening.usedMillis).remainingMillis)
    }
    @Test fun tenImagesAreAllowedAndTheEleventhIsRejected() {
        val images=ImageTrial()
        repeat(10) { assertTrue(images.consume()) }
        assertEquals(0,images.remaining)
        assertFalse(images.consume())
        assertEquals(10,images.usedCount)
    }
    @Test fun imageBalanceSurvivesReloadAndInvalidStoredValuesAreClamped() {
        val images=ImageTrial();repeat(3) { images.consume() }
        assertEquals(7,ImageTrial(images.usedCount).remaining)
        assertEquals(10,ImageTrial(-1).remaining)
        assertEquals(0,ImageTrial(999).remaining)
    }
}
