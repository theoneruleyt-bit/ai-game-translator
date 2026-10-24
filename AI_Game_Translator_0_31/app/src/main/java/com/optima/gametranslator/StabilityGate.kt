package com.optima.gametranslator

/** Two matching observations; tolerate small OCR errors in longer dialogue. */
class StabilityGate {
    var current = ""
        private set
    private var since = 0L
    private var samples = 0
    private var emptySince: Long? = null
    private var delivered = ""
    fun observe(text: String, now: Long): String? {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) {
            if (emptySince == null) emptySince = now
            if (now - emptySince!! >= 1000) delivered = ""
            current = ""; samples = 0; since = now
            return null
        }
        emptySince = null
        if (!similar(clean, current)) {
            current = clean; since = now; samples = 1; return null
        }
        current = clean; samples++
        return if (samples >= 2 && now - since >= 150 && !similar(clean, delivered)) clean else null
    }
    fun commit(text: String) { delivered = text }
    fun release(text: String) { if (similar(text, delivered)) delivered = "" }
    fun reset() { current = ""; delivered = ""; since = 0; samples = 0; emptySince = null }
    private fun key(text: String) = text.lowercase(java.util.Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]"), "")
    private fun similar(left: String, right: String): Boolean {
        val a = key(left); val b = key(right)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        // Short lines and changed numbers must remain distinct.
        if (minOf(a.length,b.length) < 20) return false
        if (a.filter { it.isDigit() } != b.filter { it.isDigit() }) return false
        val limit = (maxOf(a.length,b.length) * 0.08).toInt().coerceAtMost(4)
        if (kotlin.math.abs(a.length-b.length) > limit) return false
        var previous = IntArray(b.length+1) { it }
        for (i in a.indices) {
            val row = IntArray(b.length+1); row[0] = i+1
            for (j in b.indices) row[j+1] = minOf(row[j]+1, previous[j+1]+1,
                previous[j] + if(a[i]==b[j]) 0 else 1)
            if (row.minOrNull()!! > limit) return false
            previous = row
        }
        return previous[b.length] <= limit
    }
}
