package com.optima.gametranslator

/** Audio callbacks without new decoder output must not count as independent confirmation. */
class TranscriptConsensus {
    private var previous=emptyList<String>()
    private var stable=0
    fun observe(text: String,freshDecode: Boolean): Int {
        val words=text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            .map { it.filter(Char::isLetterOrDigit).lowercase(java.util.Locale.ROOT) }
        if(freshDecode) {
            stable=previous.zip(words).takeWhile { it.first==it.second }.size
            previous=words
        } else {
            // Retraction or a gate revision invalidates previous confirmation immediately.
            stable=minOf(stable,previous.zip(words).takeWhile { it.first==it.second }.size)
        }
        return stable
    }
    fun reset() { previous=emptyList();stable=0 }
}
