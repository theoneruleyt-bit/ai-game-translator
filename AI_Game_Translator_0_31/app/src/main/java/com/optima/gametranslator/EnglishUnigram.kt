package com.optima.gametranslator

import java.util.Base64

/** Narrow ASCII-English adapter. Unsupported Unicode is explicitly routed to the existing translator. */
class EnglishUnigram(lines: List<String>) {
    private data class Piece(val id: Int,val score: Float)
    private val pieces=HashMap<String,Piece>()
    private val decoded=HashMap<Int,String>()
    private var longest=1
    private var minimum=0f
    init {
        for(line in lines) {
            val fields=line.split('\t');require(fields.size==3)
            val id=fields[0].toInt();val score=fields[1].toFloat()
            val piece=String(Base64.getDecoder().decode(fields[2]),Charsets.UTF_8)
            decoded[id]=piece
            if(id!=1) { pieces[piece]=Piece(id,score);longest=maxOf(longest,piece.length);minimum=minOf(minimum,score) }
        }
    }
    fun encode(source: String): IntArray {
        require(source.all { it.code in 32..126||it in "\t\r\n" }) { "Unsupported source characters" }
        val clean=source.trim().replace(Regex("[ \\t\\r\\n]+")," ");require(clean.isNotEmpty())
        val text="▁"+clean.replace(' ','▁')
        val scores=FloatArray(text.length+1) { Float.NEGATIVE_INFINITY };scores[0]=0f
        val previous=IntArray(scores.size);val ids=IntArray(scores.size)
        for(i in text.indices) {
            if(!scores[i].isFinite())continue
            var single=false
            for(end in i+1..minOf(text.length,i+longest)) {
                val p=pieces[text.substring(i,end)]?:continue
                if(end==i+1)single=true
                val score=scores[i]+p.score
                if(score>scores[end]) { scores[end]=score;previous[end]=i;ids[end]=p.id }
            }
            if(!single) {
                val score=scores[i]+minimum-10f
                if(score>scores[i+1]) { scores[i+1]=score;previous[i+1]=i;ids[i+1]=1 }
            }
        }
        val reversed=mutableListOf<Int>();var end=text.length
        while(end>0) { reversed.add(ids[end]);end=previous[end] }
        val result=mutableListOf<Int>()
        for(id in reversed.asReversed())if(id!=1||result.lastOrNull()!=1)result.add(id)
        result.add(0);return result.toIntArray()
    }
    fun decode(ids: List<Int>): String {
        require(ids.none { it==1 }) { "Unknown generated token" }
        return ids.filter { it!=0&&it!=32000 }.joinToString("") { checkNotNull(decoded[it]) }
            .replace('▁',' ').trim()
    }
}
