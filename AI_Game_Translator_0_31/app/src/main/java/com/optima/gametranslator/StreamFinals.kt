package com.optima.gametranslator

/** Deduplicate SDK delivery IDs, never the words: repeated dialogue is legitimate. */
class StreamFinals {
    private val ids=LinkedHashSet<String>()
    fun accept(id: String): Boolean {
        if(id.isBlank()) return true
        if(!ids.add(id)) return false
        if(ids.size>512) ids.remove(ids.first())
        return true
    }
    fun clear() { ids.clear() }
}
