package com.optima.gametranslator

import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler

/** AEC creation or enabling can fail even when an implementation is advertised. */
class MicrophoneEcho(record: AudioRecord): AutoCloseable {
    private var effect: AcousticEchoCanceler?=null
    init {
        try {
            if(AcousticEchoCanceler.isAvailable()) {
                effect=AcousticEchoCanceler.create(record.audioSessionId)
                effect?.enabled=true
            }
        } catch(_: Exception) { effect?.release();effect=null }
    }
    override fun close() { try { effect?.release() } catch(_: Exception) {};effect=null }
}
