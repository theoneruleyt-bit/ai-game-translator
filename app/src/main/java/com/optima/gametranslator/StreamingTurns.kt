package com.optima.gametranslator

/** Only endpoint-confirmed text can be spoken. Tiny fragments can join the next turn. */
class StreamingTurns(private val graceMillis: Long=650) {
    data class Output(val text: String,val final: Boolean)
    private var pending=""
    private var due=0L
    private var continued=false
    fun update(text: String,final: Boolean,now: Long): List<Output> {
        val clean=text.trim().replace(Regex("\\s+")," ")
        if(!final) {
            if(clean.isBlank())return emptyList()
            continued=pending.isNotBlank()
            return listOf(Output(listOf(pending,clean).filter { it.isNotBlank() }.joinToString(" "),false))
        }
        if(clean.isBlank())return finish()
        val combined=listOf(pending,clean).filter { it.isNotBlank() }.joinToString(" ")
        pending=combined;continued=false
        val short=combined.split(' ').size<=3&&!combined.endsWith('.')&&!combined.endsWith('!')&&!combined.endsWith('?')
        if(short) { due=now+graceMillis;return listOf(Output(combined,false)) }
        return finish()
    }
    fun tick(now: Long): List<Output> = if(pending.isNotBlank()&&!continued&&now>=due)finish() else emptyList()
    private fun finish(): List<Output> {
        val text=pending;pending="";continued=false;due=0
        return if(text.isBlank())emptyList() else listOf(Output(text,true))
    }
}
