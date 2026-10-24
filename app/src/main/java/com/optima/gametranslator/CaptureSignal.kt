package com.optima.gametranslator

/** Measurements only. Quiet audio is reported, never discarded by an amplitude gate. */
data class CaptureSignal(val rms: Double,val clippedPortion: Double,val backlogMillis: Long) {
    companion object {
        fun inspect(samples: FloatArray,backlogMillis: Long): CaptureSignal {
            if(samples.isEmpty())return CaptureSignal(0.0,0.0,backlogMillis.coerceAtLeast(0))
            val rms=kotlin.math.sqrt(samples.sumOf { it.toDouble()*it }/samples.size)
            val clipped=samples.count { kotlin.math.abs(it)>=.99f }.toDouble()/samples.size
            return CaptureSignal(rms,clipped,backlogMillis.coerceAtLeast(0))
        }
    }
}
