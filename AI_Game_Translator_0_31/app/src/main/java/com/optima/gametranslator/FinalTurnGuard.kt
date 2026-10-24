package com.optima.gametranslator

/** Deduplicate event IDs, never deduplicate words: real repeated dialogue must remain. */
class FinalTurnGuard {
    private val recent=java.util.ArrayDeque<String>()
    fun accept(id: String?): Boolean {
        if(id==null)return true
        if(id in recent)return false
        recent.addLast(id);while(recent.size>128)recent.removeFirst();return true
    }
    fun reset()=recent.clear()
}
