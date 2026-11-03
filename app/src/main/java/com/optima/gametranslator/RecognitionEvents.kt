package com.optima.gametranslator

/** Empty final endpoints still clear a previous draft; only delivery IDs are deduplicated. */
class RecognitionEvents {
    data class Event(val text: String,val final: Boolean)
    private val finals=StreamFinals()
    fun accept(text: String,final: Boolean,id: String=""): Event? {
        if(final&&!finals.accept(id))return null
        val clean=text.trim()
        if(!final&&clean.isEmpty())return null
        return Event(clean,final)
    }
}
