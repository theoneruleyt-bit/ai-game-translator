package com.optima.gametranslator

/** Opt-in memory-only text diagnostics; no waveform, identifiers or network writes. */
class ListeningTrace(private val capacity: Int=128) {
    data class Event(val at: Long,val stage: String,val text: String,val detail: String)
    private val entries=java.util.ArrayDeque<Event>()
    var enabled=false
        set(value) { field=value;if(!value)entries.clear() }
    fun record(now: Long,stage: String,text: String="",detail: String="") {
        if(!enabled)return
        entries.addLast(Event(now,stage.take(60),text.take(500),detail.take(200)))
        while(entries.size>capacity)entries.removeFirst()
    }
    fun snapshot()=entries.toList()
    fun clear()=entries.clear()
}
