package com.optima.gametranslator

/** ASR words are not evidence that someone actually spoke. */
class SpeechEvidence {
    private var detected=false
    fun observe(speech: Boolean) { if(speech)detected=true }
    fun allowsPreview()=detected
    fun endTurn(): Boolean { val result=detected;detected=false;return result }
}

/** Give VAD exact native window sizes, independent of microphone read sizes. */
class PcmWindows(private val size: Int=512) {
    private val pending=FloatArray(size)
    private var count=0
    fun accept(samples: FloatArray,window: (FloatArray)->Unit) {
        var offset=0
        while(offset<samples.size) {
            val n=minOf(size-count,samples.size-offset)
            samples.copyInto(pending,count,offset,offset+n);count+=n;offset+=n
            if(count==size) { window(pending.copyOf());count=0 }
        }
    }
}
