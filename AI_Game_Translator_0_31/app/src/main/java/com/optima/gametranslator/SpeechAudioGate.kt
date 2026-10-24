package com.optima.gametranslator

/** Keep 400 ms of lead-in. Idle noise is never passed to ASR; active speech retains all trailing audio. */
class SpeechAudioGate(private val prefixSamples: Int=6400) {
    private val prefix=java.util.ArrayDeque<FloatArray>()
    private var buffered=0
    private var active=false
    fun accept(samples: FloatArray,speech: Boolean): List<FloatArray> {
        if(active)return listOf(samples)
        prefix.addLast(samples);buffered+=samples.size
        while(buffered>prefixSamples) {
            val first=prefix.removeFirst();val excess=buffered-prefixSamples
            if(first.size<=excess)buffered-=first.size
            else { prefix.addFirst(first.copyOfRange(excess,first.size));buffered-=excess }
        }
        if(!speech)return emptyList()
        active=true;val result=prefix.toList();prefix.clear();buffered=0;return result
    }
    fun endTurn() { active=false;prefix.clear();buffered=0 }
}
