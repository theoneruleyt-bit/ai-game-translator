package com.optima.gametranslator

/** Audio time, not changing decoder text, determines whether a turn is still alive. */
class SpeechTurnBoundary(private val settleMillis: Long=700,private val silenceLimit: Long=1100) {
    private var lastSpeech: Long?=null
    private var trusted=""
    fun observe(speech: Boolean,now: Long) { if(speech)lastSpeech=now }
    fun accept(hypothesis: String,now: Long): String {
        val last=lastSpeech?:return ""
        // Allow decoder look-ahead, then freeze the last audio-supported hypothesis.
        if(now-last<=settleMillis)trusted=hypothesis.trim()
        return trusted
    }
    fun mustEnd(now: Long)=lastSpeech?.let { now-it>=silenceLimit }?:false
    fun reset() { lastSpeech=null;trusted="" }
    companion object {
        // Only digital silence / a few PCM quantization steps, not a quiet-voice cutoff.
        fun hasSignal(samples: FloatArray)=samples.isNotEmpty()&&
            samples.sumOf { it.toDouble()*it }/samples.size>1e-8
    }
}
