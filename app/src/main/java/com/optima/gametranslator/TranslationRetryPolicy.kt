package com.optima.gametranslator

/** Busy is backpressure, not a failed inference. Bound waiting and back off real errors. */
object TranslationRetryPolicy {
    fun delay(busy: Boolean,attempts: Int,busyMillis: Long): Long? = when {
        busy && busyMillis < 6000 -> 250L
        busy -> null
        attempts < 2 -> 300L * (attempts + 1)
        else -> null
    }
}
