package com.optima.gametranslator

/** Duration-based controller. Values are estimates until calibrated by completed playback. */
class AdaptiveTempo {
    private var millisPerUnit=145.0
    private var previousRate=1f
    fun units(text: String): Int {
        val syllables=text.count { it.lowercaseChar() in "aeıioöuü" }
        val words=text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return maxOf(1,syllables,words*2)+text.count { it in ",;:.!?" }*2
    }
    fun estimateMillis(text: String): Long=(units(text)*millisPerUnit).toLong().coerceAtLeast(300)
    fun choose(text: String,base: Float,waitingMillis: Long,ageMillis: Long,sourceMillis: Long,enabled: Boolean,maxRate: Float=1.5f): Float {
        if(!enabled)return base.coerceIn(.7f,1.5f)
        val ceiling=maxRate.coerceIn(1.5f,2f).toDouble()
        val duration=estimateMillis(text).toDouble()
        val sourceTarget=if(sourceMillis>=500) duration/sourceMillis else base.toDouble()
        val backlog=(waitingMillis.coerceAtLeast(0)/12000.0+ageMillis.coerceAtLeast(0)/20000.0).coerceAtMost(if(ceiling>1.5) .75 else .35)
        val target=(maxOf(base.toDouble(),sourceTarget)+backlog).coerceIn(.85,ceiling)
        // Slow speech is allowed only with a measured source window and no backlog.
        val desired=if(sourceMillis>=500&&waitingMillis==0L&&ageMillis<1500) (sourceTarget+backlog).coerceIn(.85,ceiling) else target
        val result=desired.coerceIn((previousRate-.15).toDouble(),(previousRate+if(ceiling>1.5) .35 else .20).toDouble()).toFloat().coerceIn(.85f,ceiling.toFloat())
        previousRate=result;return result
    }
    fun completed(text: String,rate: Float,elapsedMillis: Long) {
        if(elapsedMillis !in 250..120000)return
        val observed=(elapsedMillis*rate/units(text)).toDouble().coerceIn(60.0,350.0)
        millisPerUnit=.8*millisPerUnit+.2*observed
    }
    fun reset() { previousRate=1f }
}
