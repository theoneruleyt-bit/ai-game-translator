package com.optima.gametranslator

/** FIFO with explicit overflow: accepted entries are never replaced or age-dropped. */
class LineQueue(private val capacity: Int = 32) {
    data class Entry(val text: String, val seenAt: Long,val speakerSlot: Int=0,val speakerName: String="",val displayRevision: Long=-1,val sourceMillis: Long=0,val attempts: Int=0,val retryStartedAt: Long=-1)
    private val entries = java.util.ArrayDeque<Entry>()
    val size: Int get() = entries.size
    fun offer(text: String, now: Long, speakerSlot: Int=0, speakerName: String="",displayRevision: Long=-1,sourceMillis: Long=0): Boolean {
        if (entries.size >= capacity) return false
        entries.addLast(Entry(text, now,speakerSlot,speakerName,displayRevision,sourceMillis)); return true
    }
    fun take(): Entry? = entries.pollFirst()
    fun clear() = entries.clear()
}
