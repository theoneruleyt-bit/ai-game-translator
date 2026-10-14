package com.optima.gametranslator

/** Duration alone must not reset a speaking user. Long turns also require a pause. */
data class SpeechEndpointPolicy(val trailingSilence: Float,val longTurnSilence: Float,val longTurnSeconds: Float,val paths: Int) {
    companion object {
        fun forMode(mode: Int)=when(mode) {
            0 -> SpeechEndpointPolicy(.85f,.35f,18f,2)
            2 -> SpeechEndpointPolicy(1.25f,.5f,25f,4)
            else -> SpeechEndpointPolicy(1.05f,.4f,20f,4)
        }
    }
}
