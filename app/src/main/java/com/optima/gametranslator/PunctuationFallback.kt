package com.optima.gametranslator

/** Optional punctuation must never stop microphone recognition when its native model fails. */
class PunctuationFallback(private val process: (String)->String,
                          private val release: ()->Unit,
                          private val onDisabled: ()->Unit): AutoCloseable {
    private var available=true
    private var released=false
    fun apply(text: String): String {
        if(!available)return text
        return try { process(text) }
        catch(_: Exception) { fallback(text) }
        catch(_: LinkageError) { fallback(text) }
        catch(_: OutOfMemoryError) { fallback(text) }
    }
    private fun fallback(text: String): String {
        available=false
        close()
        onDisabled()
        return text
    }
    override fun close() {
        available=false
        if(released)return
        released=true
        try { release() }
        catch(_: Exception) { }
        catch(_: LinkageError) { }
        catch(_: OutOfMemoryError) { }
    }
}
