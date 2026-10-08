package com.optima.gametranslator

/** Routes explicit subtitle labels only. It never infers identity/gender from audio. */
class SpeakerRouter(private val capacity: Int=16) {
    data class Segment(val text: String,val slot: Int,val name: String)
    private val names=linkedMapOf<String,Int>()
    private val pattern=Regex("^\\s*(?:\\[([\\p{L}][\\p{L}\\p{M} .'-]{0,23})]|([\\p{L}][\\p{L}\\p{M} .'-]{0,23}):)\\s*(.*)$")
    private val metadata=setOf("objective","mission","warning","note","time","score","hp","ammo")
    fun bindings(): Map<String,Int> = names.toMap()
    fun clear() = names.clear()
    fun restore(saved: Map<String,Int>) {
        names.clear()
        saved.filter { it.value in 1..capacity }.entries.take(capacity).forEach {
            names[it.key.lowercase(java.util.Locale.ROOT)]=it.value
        }
    }
    fun route(text: String,manualSlot: Int,enabled: Boolean): List<Segment> {
        if(!enabled)return listOf(Segment(text,manualSlot,""))
        val output=mutableListOf<Segment>()
        var slot=manualSlot;var name="";var body=StringBuilder()
        fun flush() { if(body.isNotBlank()) output.add(Segment(body.toString().trim(),slot,name));body=StringBuilder() }
        for(line in text.lines()) {
            val match=pattern.matchEntire(line)
            val label=match?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }.orEmpty().trim()
            val key=label.lowercase(java.util.Locale.ROOT)
            if(match!=null&&key !in metadata) {
                flush();name=label
                slot=names[key] ?: ((1..capacity).firstOrNull { it !in names.values } ?: manualSlot).also {
                    if(names.size<capacity)names[key]=it
                }
                body.append(match.groupValues[3])
            } else { if(body.isNotEmpty())body.append('\n');body.append(line) }
        }
        flush();return output
    }
}
