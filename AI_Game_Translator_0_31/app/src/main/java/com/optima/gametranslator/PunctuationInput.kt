package com.optima.gametranslator

/** Zipformer transcripts are uppercase; the bundled case/punctuation model expects lowercase words. */
object PunctuationInput {
    fun prepare(text: String): String = text.lowercase(java.util.Locale.ROOT)
}
