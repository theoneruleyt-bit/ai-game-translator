package com.optima.gametranslator

/** Spoken output is time-sensitive. Written translations are retained independently. */
class LiveReadPolicy {
    private var lastSpeechAt: Long?=null
    fun observeSpeech(now: Long) { lastSpeechAt=now }
    fun allows(sourceAt: Long,now: Long,continuation: Boolean): Boolean {
        if(continuation)return true
        if(now-sourceAt>6000)return false
        val last=lastSpeechAt
        return !(last!=null&&now-last>2500&&now-sourceAt>2000)
    }
    fun reset() { lastSpeechAt=null }
}
