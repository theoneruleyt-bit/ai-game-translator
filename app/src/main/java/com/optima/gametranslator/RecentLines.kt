package com.optima.gametranslator

/** Suppress camera OCR oscillation A→B→A without permanently suppressing dialogue. */
class RecentLines {
    private val lines=java.util.LinkedHashMap<String,Long>()
    private var emptySince: Long?=null
    private fun key(text: String)=text.lowercase(java.util.Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"),"")
    fun shouldSubmit(text: String,now: Long): Boolean {
        val k=key(text)
        lines.entries.removeAll { now-it.value>8000 }
        if(k.isEmpty()||lines.containsKey(k))return false
        lines[k]=now
        while(lines.size>8)lines.remove(lines.keys.first())
        return true
    }
    fun observeVisibility(text: String,now: Long) {
        if(text.isNotBlank()) {
            emptySince=null
            val visible=key(text)
            // Refresh real visibility, including lines hidden by the stationary-text filter.
            lines.keys.toList().forEach { k ->
                if(k==visible||(k.length>=20&&visible.contains(k)))lines[k]=now
            }
        }
        else {
            if(emptySince==null)emptySince=now
            if(now-emptySince!!>=1000)lines.clear()
        }
    }
    fun forget(text: String) { lines.remove(key(text)) }
    fun clear() { lines.clear();emptySince=null }
}
