package com.optima.gametranslator

/** Commit a stable prefix while retaining a revisable tail. Finals only emit the remainder. */
class LiveSpeechSegments {
    data class Update(val chunks: List<String>,val preview: String)
    private var previous=emptyList<String>()
    private var committed=0
    private var stableSince=-1L
    fun update(text: String,final: Boolean,now: Long): Update {
        val words=text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if(final) {
            val tail=words.drop(committed).joinToString(" ")
            reset()
            return Update(if(tail.isBlank()) emptyList() else listOf(tail),"")
        }
        val common=words.zip(previous).takeWhile { (a,b) -> a.equals(b,true) }.size
        val available=(common-committed-1).coerceAtLeast(0)
        if(available==0) stableSince=-1
        else if(stableSince<0) stableSince=now
        val chunks=mutableListOf<String>()
        if(available>=3 && (now-stableSince>=240 || available>=8)) {
            val count=available.coerceAtMost(8)
            chunks.add(words.drop(committed).take(count).joinToString(" "))
            committed+=count;stableSince=-1
        }
        previous=words
        return Update(chunks,words.drop(committed).joinToString(" "))
    }
    fun reset() { previous=emptyList();committed=0;stableSince=-1 }
}
