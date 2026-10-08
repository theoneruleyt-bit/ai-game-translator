package com.optima.gametranslator

/** Suppress consecutive duplicate lines, but allow genuine repetition after absence. */
class StabilityGate {
    var current = ""
        private set
    private var since = 0L
    private var emptySince: Long? = null
    private var delivered = ""
    fun observe(text: String, now: Long): String? {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) {
            if (emptySince == null) emptySince = now
            if (now - emptySince!! >= 1500) delivered = ""
            current = ""; since = now
            return null
        }
        emptySince = null
        if (clean != current) { current = clean; since = now; return null }
        return if (now - since >= 600 && clean != delivered) clean else null
    }
    fun commit(text: String) { delivered = text }
    fun reset() { current = ""; delivered = ""; since = 0; emptySince = null }
}
