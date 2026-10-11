package com.optima.gametranslator

/** Count only images that produced a translation. Cancellation and OCR failure do not count. */
class ImageTrial(used: Int=0) {
    companion object { const val LIMIT=10 }
    var usedCount=used.coerceIn(0,LIMIT)
        private set
    val remaining get()=LIMIT-usedCount
    fun consume(): Boolean {
        if(remaining==0)return false
        usedCount++
        return true
    }
}
