package com.optima.gametranslator

/** FIFO with explicit overflow: accepted entries are never replaced or age-dropped. */
class LineQueue(private val capacity: Int = 32) {
    data class Entry(val text: String, val seenAt: Long)
    private val entries = java.util.ArrayDeque<Entry>()
    val size: Int get() = entries.size
    fun offer(text: String, now: Long): Boolean {
        if (entries.size >= capacity) return false
        entries.addLast(Entry(text, now)); return true
    }
    fun take(): Entry? = entries.pollFirst()
    fun clear() = entries.clear()
}
