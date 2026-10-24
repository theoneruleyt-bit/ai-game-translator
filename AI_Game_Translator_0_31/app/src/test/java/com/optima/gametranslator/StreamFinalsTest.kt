package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class StreamFinalsTest {
    @Test fun duplicateDeliveryIsRejected() {
        val gate=StreamFinals()
        assertTrue(gate.accept("segment-1"))
        assertFalse(gate.accept("segment-1"))
    }
    @Test fun repeatedDialogueWithDifferentIdsIsKept() {
        val gate=StreamFinals()
        assertTrue(gate.accept("first-delivery"))
        assertTrue(gate.accept("second-delivery"))
    }
    @Test fun newSessionCanReuseAnId() {
        val gate=StreamFinals()
        gate.accept("1");gate.clear()
        assertTrue(gate.accept("1"))
    }
    @Test fun retentionIsBounded() {
        val gate=StreamFinals()
        repeat(513) { assertTrue(gate.accept(it.toString())) }
        assertFalse(gate.accept("512"))
        assertTrue(gate.accept("0"))
    }
}
