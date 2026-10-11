package com.optima.gametranslator

/** Preview is revisable. Only the recognizer endpoint commits a complete utterance. */
class LiveSpeechSegments {
    data class Update(val chunks: List<String>,val preview: String)
    fun update(text: String,final: Boolean,@Suppress("UNUSED_PARAMETER") now: Long): Update {
        val complete=text.trim().replace(Regex("\\s+")," ")
        return if(final) Update(if(complete.isBlank()) emptyList() else listOf(complete),"")
        else Update(emptyList(),complete)
    }
    fun reset() { /* No word counter can leak into another utterance. */ }
}
