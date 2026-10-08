package com.optima.gametranslator

/** At most one waiting line. A newer stable subtitle replaces the waiting one. */
class LatestLine(private val maxAgeMs: Long = 3000) {
    data class Entry(val text: String, val seenAt: Long)
    private var pending: Entry? = null
    fun offer(text: String, now: Long) { pending = Entry(text, now) }
    fun take(now: Long): Entry? {
        val entry = pending; pending = null
        return entry?.takeIf { now - it.seenAt in 0..maxAgeMs }
    }
    fun clear() { pending = null }
}
