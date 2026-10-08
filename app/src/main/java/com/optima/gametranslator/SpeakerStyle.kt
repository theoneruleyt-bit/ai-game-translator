package com.optima.gametranslator

/** Style variants change pitch/rate, not identity, gender, accent or the underlying voice. */
object SpeakerStyle {
    val names=arrayOf("Doğal","Daha kalın ton","Daha ince ton","Sakin","Enerjik","Anlatıcı")
    fun pitch(style: Int)=when(style) { 1->.88f;2->1.12f;3->.97f;4->1.05f;5->.94f;else->1f }
    fun rate(style: Int)=when(style) { 3->.95f;4->1.08f;5->.98f;else->1f }
    fun catchUpRate(base: Float,pending: Int,enabled: Boolean): Float {
        if(!enabled)return base
        return (base+when { pending>=4->.30f;pending>=2->.18f;pending>=1->.08f;else->0f }).coerceAtMost(maxOf(base,1.65f))
    }
}
