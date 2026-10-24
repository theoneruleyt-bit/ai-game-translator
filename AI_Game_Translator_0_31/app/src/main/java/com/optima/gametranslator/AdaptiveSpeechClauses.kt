package com.optima.gametranslator

/** Commit stable meaning groups without resetting the recognizer or replaying its cumulative prefix. */
class AdaptiveSpeechClauses(private val enabled: Boolean=true) {
    data class Output(val text: String,val final: Boolean)
    private var consumed=emptyList<String>()
    private var rightAnchor=emptyList<String>()
    private var modelInput=""
    private var modelOutput=""
    private var modelAt=Long.MIN_VALUE
    private var candidate=""
    private var candidateSince=0L
    private var tail=""
    private var tailSince=0L
    private fun key(word: String)=word.filter { it.isLetterOrDigit() }.lowercase(java.util.Locale.ROOT)
    private fun words(text: String)=text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    private data class Cursor(val count: Int,val reliable: Boolean)
    private fun cursor(current: List<String>): Cursor {
        if(consumed.isEmpty())return Cursor(0,true)
        if(current.size>=consumed.size&&current.take(consumed.size)==consumed)return Cursor(consumed.size,true)
        // Right context is still unspoken. It survives insertions/deletions in an already spoken prefix.
        if(rightAnchor.size>=2) {
            val hits=(maxOf(0,consumed.size-12)..minOf(current.size-rightAnchor.size,consumed.size+12))
                .filter { current.subList(it,it+rightAnchor.size)==rightAnchor }
            if(hits.size==1)return Cursor(hits.single(),true)
        }
        // A unique spoken suffix anchors prefix deletion even without right context.
        if(consumed.size>=3) {
            val suffix=consumed.takeLast(3)
            val hits=(0..current.size-3).filter { current.subList(it,it+3)==suffix }
            if(hits.size==1)return Cursor(hits.single()+3,true)
        }
        // A late rewrite cannot be retracted from TTS. Align once, but suspend early commits until endpoint.
        val max=minOf(current.size,consumed.size+12)
        var row=IntArray(max+1) { it }
        for((i,old) in consumed.withIndex()) {
            val next=IntArray(max+1);next[0]=i+1
            for(j in 1..max)next[j]=minOf(row[j]+1,next[j-1]+1,row[j-1]+if(old==current[j-1])0 else 1)
            row=next
        }
        val choices=(maxOf(0,consumed.size-12)..max).toList()
        val at=choices.minWithOrNull(compareBy<Int> { row[it] }
            .thenBy { if(it>0&&current[it-1]==consumed.last())0 else 1 }
            .thenBy { kotlin.math.abs(it-consumed.size) })?:minOf(current.size,consumed.size)
        // An unanchored rewrite must not move behind words already spoken.
        return Cursor(maxOf(at,minOf(consumed.size,current.size)),false)
    }
    private val dangling=setOf("a","an","the","and","but","or","because","if","when","while","although","unless","until","that","which","who","whose","where","to","of","for","with","from","at","in","on","by","than","as","my","your","his","her","our","their","this","these","those")
    private val dependent=setOf("if","when","while","although","unless","until","because","whereas","since")
    private val predicates=setOf("am","is","are","was","were","have","has","had","do","does","did","can","could","will","would","should","shall","may","might","must","need","needs","want","wants","know","knows","think","thinks","said","says","went","saw","got","told")
    private val imperatives=setOf("go","come","get","take","bring","look","find","wait","stop","run","open","close","follow","listen","stay","leave","keep","move","turn","tell","give","let","put","pick","watch","hold","meet","call","help","save","drive","shoot","drop","check","try","remember","forget","use","make","hide")
    private fun complete(part: List<String>,sentence: Boolean): Boolean {
        val w=part.map(::key);if(w.size<if(sentence)2 else 4)return false
        if(w.last() in dangling)return false
        if(!sentence&&w.first() in dependent)return false
        if(w.last() in setOf("mr","mrs","ms","dr","prof","jr","sr","vs","eg","ie"))return false
        return sentence||w.first() in imperatives||w.any { it in predicates }||
            (w.size>=3&&w.first() in setOf("i","we","you","they","he","she","it")&&w[1] !in dangling)
    }
    private data class Mark(val end: Int,val sentence: Boolean)
    private data class Punctuated(val words: List<String>,val text: String) {
        private val positions=text.indices.filter { text[it].isLetterOrDigit() }
        private val counts=words.runningFold(0) { count,word -> count+word.count { it.isLetterOrDigit() } }
        val valid=words.joinToString("").filter { it.isLetterOrDigit() }.lowercase(java.util.Locale.ROOT)==
            text.filter { it.isLetterOrDigit() }.lowercase(java.util.Locale.ROOT)
        fun marks(): List<Mark> {
            if(!valid)return emptyList()
            return (1..words.size).mapNotNull { n ->
                val count=counts[n];if(count==0)return@mapNotNull null
                val end=positions[count-1]+1;val next=positions.getOrNull(count)?:text.length
                val punctuation=text.substring(end,next)
                when {
                    punctuation.any { it in ".!?" } -> Mark(n,true)
                    punctuation.any { it in ",;:" } -> Mark(n,false)
                    else -> null
                }
            }
        }
        fun prefix(n: Int): String {
            val end=positions.getOrNull(counts[n])?:text.length
            return text.substring(0,end).trim()
        }
    }
    fun update(text: String,now: Long,quietMillis: Long,final: Boolean,punctuate: (String)->String): List<Output> {
        return updateStable(text,now,quietMillis,final,Int.MAX_VALUE,punctuate)
    }
    fun updateStable(text: String,now: Long,quietMillis: Long,final: Boolean,stableWordLimit: Int,punctuate: (String)->String): List<Output> {
        val all=words(text);val normalized=all.map(::key);val cursor=cursor(normalized)
        val rest=all.drop(cursor.count)
        if(final) {
            val remaining=rest.joinToString(" ")
            val rendered=if(enabled&&remaining.isNotBlank()) {
                val p=Punctuated(rest,punctuate(remaining));if(p.valid)p.text else remaining
            } else remaining
            reset();return listOf(Output(rendered,true))
        }
        val plain=rest.joinToString(" ")
        if(!enabled)return if(plain.isBlank())emptyList() else listOf(Output(plain,false))
        if(plain!=tail) { tail=plain;tailSince=now }
        if(rest.isEmpty()) { candidate="";return emptyList() }
        // A bounded analysis window is not a word-count cut: without a boundary nothing is committed.
        val window=rest.take(96);val input=window.joinToString(" ")
        if(input!=modelInput&&(modelAt==Long.MIN_VALUE||now-modelAt>=300)) {
            modelInput=input;modelOutput=punctuate(input);modelAt=now
        }
        // Appended ASR words do not invalidate an earlier internal boundary. A rewrite does.
        val prediction=if(input==modelInput||modelInput.isNotBlank()&&input.startsWith("$modelInput "))
            Punctuated(words(modelInput),modelOutput) else null
        if(prediction==null)return listOf(Output(plain,false))
        val mark=if(cursor.reliable)prediction?.marks()?.firstOrNull { boundary ->
            val lookAhead=rest.size-boundary.end
            complete(rest.take(boundary.end),boundary.sentence)&&
                (lookAhead>=2||boundary.end==rest.size)
        } else null
        // The pause path also works when punctuation predicts no terminal symbol.
        val pause=mark==null&&cursor.reliable&&complete(rest,false)&&rest.size<=96
        val cut=mark?.end?:if(pause)rest.size else 0
        if(cut==0) { candidate="";return listOf(Output(plain,false)) }
        val next=rest.take(cut).joinToString(" ") { key(it) }+if(mark?.sentence==true)"." else ","
        if(next!=candidate) { candidate=next;candidateSince=now }
        val boundaryReady=mark!=null&&rest.size-cut>=2||quietMillis>=120&&now-tailSince>=300
        if(cursor.count+cut>stableWordLimit||!boundaryReady||now-candidateSince<280)return listOf(Output(plain,false))
        var committed=if(mark!=null&&prediction!=null)prediction.prefix(cut) else rest.take(cut).joinToString(" ")
        if(committed.lastOrNull() !in listOf('.', '!', '?', ',', ';', ':'))committed+="."
        consumed=normalized.take(cursor.count+cut);rightAnchor=normalized.drop(consumed.size).take(3)
        candidate="";modelInput="";modelOutput="";modelAt=Long.MIN_VALUE;tail=""
        val after=rest.drop(cut).joinToString(" ")
        return listOf(Output(committed,true))+if(after.isBlank())emptyList() else listOf(Output(after,false))
    }
    fun reset() {
        consumed=emptyList();rightAnchor=emptyList();modelInput="";modelOutput="";modelAt=Long.MIN_VALUE
        candidate="";candidateSince=0;tail="";tailSince=0
    }
}
