package com.optima.gametranslator

import android.content.Context
import java.util.Locale

/** Whole-phrase matches only; slang never replaces substrings in unrelated sentences. */
class ConversationLexicon private constructor(rows: List<String>) {
    constructor(context: Context): this(context.assets.open("conversation-en-tr.tsv").bufferedReader().use { it.readLines() })
    constructor(phrases: Map<String,String>): this(phrases.map { "${it.key}\t${it.value}" })
    private val english=LinkedHashMap<String,String>()
    private val turkish=LinkedHashMap<String,String>()
    init {
        run {
            rows.filter { it.isNotBlank()&&!it.startsWith("#") }.forEach { line ->
                val pair=line.split('\t');require(pair.size==2)
                english[key(pair[0])]=pair[1]
                turkish.putIfAbsent(key(pair[1]),pair[0])
            }
        }
    }
    fun exact(text: String,reverse: Boolean)= (if(reverse) turkish else english)[key(text)]
    fun normalizeEnglish(text: String): String {
        var result=text
        for((word,expanded) in expansions) result=result.replace(Regex("(?i)\\b${Regex.escape(word)}\\b"),expanded)
        return result
    }
    companion object {
        fun key(text: String)=text.lowercase(Locale.ROOT).replace('’','\'')
            .trim().trimEnd('.',',','!','?',';',':').replace(Regex("\\s+")," ")
        private val expansions=linkedMapOf("gonna" to "going to","wanna" to "want to",
            "gotta" to "have to","lemme" to "let me","gimme" to "give me",
            "dunno" to "do not know","kinda" to "kind of","sorta" to "sort of",
            "outta" to "out of","lotta" to "lot of","coulda" to "could have",
            "shoulda" to "should have","woulda" to "would have")
    }
}
