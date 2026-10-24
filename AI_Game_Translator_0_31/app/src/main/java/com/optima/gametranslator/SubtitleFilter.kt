package com.optima.gametranslator

/** Camera-only filters. Gallery, single-frame and microphone text remain untouched. */
class SubtitleFilter {
    private data class Track(val first: Long,var last: Long)
    private val fixed=mutableMapOf<String,Track>()
    fun eligible(text: String): Boolean {
        val clean=text.trim()
        if(clean.isBlank())return false
        // Clock/progress values, pure counters, and common standalone HUD counters.
        if(Regex("\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*/\\s*\\d{1,2}:\\d{2}(?::\\d{2})?)?").matches(clean))return false
        if(Regex("[\\d\\s:/%.,+\\-]+").matches(clean))return false
        if(Regex("(?i)(?:HP|MP|XP|FPS|PING|AMMO)\\s*[:=]?\\s*\\d+(?:\\s*/\\s*\\d+)?(?:\\s*(?:ms|%))?").matches(clean))return false
        return true
    }
    fun keep(text: String,x: Float,y: Float,now: Long): Boolean {
        if(!eligible(text))return false
        val clean=text.trim()
        fixed.entries.removeAll { now-it.value.last>1500 }
        val normalized=clean.lowercase(java.util.Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"),"")
        val key="${(x*20).toInt()}:${(y*20).toInt()}:$normalized"
        val track=fixed[key]?.takeIf { now-it.last<=1500 } ?: Track(now,now).also { fixed[key]=it }
        track.last=now
        return now-track.first<8000
    }
    fun clear()=fixed.clear()
}
