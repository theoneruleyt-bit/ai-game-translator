package com.optima.gametranslator

/** Ignore old callbacks; bound engines which accept speak but never complete it. */
class PlaybackDeadline {
    private var current: String?=null
    private var deadline=0L
    private var duration=30000L
    fun begin(id: String,now: Long,estimatedMillis: Long) {
        current=id;duration=(estimatedMillis.coerceIn(0,60000)*4+15000).coerceIn(30000,180000)
        deadline=now+12000
    }
    fun started(id: String,now: Long) { if(current==id)deadline=now+duration }
    fun expired(id: String,now: Long)=current==id&&now>=deadline
    fun clear() { current=null }
}
