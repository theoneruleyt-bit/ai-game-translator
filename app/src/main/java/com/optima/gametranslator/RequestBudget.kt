package com.optima.gametranslator

/** Timeouts do not free native capacity; only physical completion does. */
class RequestBudget {
    enum class Lane { FINAL, PREVIEW }
    private val active=mutableMapOf<Long,Lane>()
    private var sequence=0L
    fun acquire(lane: Lane): Long? {
        val limit=if(lane==Lane.PREVIEW)1 else 2
        if(active.values.count { it==lane }>=limit)return null
        val id=++sequence;active[id]=lane;return id
    }
    fun release(id: Long) { active.remove(id) }
    fun count(lane: Lane)=active.values.count { it==lane }
}
