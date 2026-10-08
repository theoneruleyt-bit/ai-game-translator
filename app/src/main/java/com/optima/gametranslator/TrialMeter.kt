package com.optima.gametranslator

/** Counts active capture only, with a monotonic clock. Shared by camera and microphone. */
class TrialMeter(used: Long=0) {
    companion object { const val LIMIT=180000L }
    var usedMillis=used.coerceIn(0,LIMIT)
        private set
    private var previous: Long?=null
    val remainingMillis get()=LIMIT-usedMillis
    val running get()=previous!=null
    fun start(now: Long) { if(previous==null&&remainingMillis>0)previous=now }
    fun tick(now: Long) {
        val last=previous?:return
        usedMillis=(usedMillis+(now-last).coerceAtLeast(0)).coerceAtMost(LIMIT)
        previous=if(remainingMillis==0L) null else maxOf(last,now)
    }
    fun reconcile(used: Long) { usedMillis=maxOf(usedMillis,used.coerceIn(0,LIMIT));if(remainingMillis==0L)previous=null }
    fun stop(now: Long) { tick(now);previous=null }
}
