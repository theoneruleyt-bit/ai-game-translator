package com.optima.gametranslator

import android.content.Context
import java.util.Locale

/** Whole-phrase matches only; slang never replaces substrings in unrelated sentences. */
class ConversationLexicon private constructor(rows: List<String>) {
    constructor(context: Context,assetName: String="conversation-en-tr.tsv"): this(context.assets.open(assetName).bufferedReader().use { it.readLines() })
    constructor(phrases: Map<String,String>): this(phrases.map { "${it.key}\t${it.value}" })
    private val english=LinkedHashMap<String,String>()
    private val turkish=LinkedHashMap<String,String>()
    private val englishQuestions=mutableSetOf<String>()
    init {
        run {
            rows.filter { it.isNotBlank()&&!it.startsWith("#") }.forEach { line ->
                val pair=line.split('\t');require(pair.size==2)
                english[key(pair[0])]=pair[1]
                if(pair[0].trim().endsWith('?')||Regex("(?i)^(what|who|where|when|why|how|which|whose|are|is|am|was|were|do|does|did|can|could|will|would|should|have|has|must)\\b").containsMatchIn(pair[0].trim()))englishQuestions.add(key(pair[0]))
                turkish.putIfAbsent(key(pair[1]),pair[0])
            }
        }
    }
    fun exact(text: String,reverse: Boolean)= (if(reverse) turkish else english)[key(text)]
    private val dialogueAliases by lazy {
        english.entries.groupBy { key(normalizeDialogueEnglish(it.key)) }
            .mapNotNull { (key,entries) -> entries.map { it.value }.distinct().singleOrNull()?.let { key to it } }.toMap()
    }
    private fun dialogueExact(text: String): String? {
        if(text.trim().endsWith('?')&&key(text) !in englishQuestions)return null
        exact(text,false)?.let { return it }
        return dialogueAliases[key(normalizeDialogueEnglish(text))]
    }
    fun exactSentences(text: String): String? {
        dialogueExact(text)?.let { return it }
        val sentences=text.trim().split(Regex("(?<=[.!?])\\s+"))
        if(sentences.size<2)return null
        val translated=sentences.map { sentence ->
            val translated=dialogueExact(sentence)?:return null
            if(translated.lastOrNull() in listOf('.', '!', '?'))translated
            else translated+(sentence.lastOrNull()?.takeIf { it in ".!?" }?:'.')
        }
        return translated.joinToString(" ")
    }
    fun normalizeEnglish(text: String): String {
        var result=text
        for((pattern,expanded) in expansionPatterns) result=result.replace(pattern,expanded)
        return result
    }
    fun normalizeDialogueEnglish(text: String): String {
        var result=normalizeEnglish(text.replace('’','\''))
        for((pattern,expanded) in dialoguePatterns)result=result.replace(pattern,expanded)
        return result
    }
    companion object {
        fun key(text: String)=text.lowercase(Locale.ROOT).replace('’','\'')
            .trim().trimEnd('.',',','!','?',';',':').replace(Regex("\\s+")," ")
        private val dialogueExpansions=linkedMapOf(
            "don't" to "do not","dont" to "do not","doesn't" to "does not","doesnt" to "does not",
            "didn't" to "did not","didnt" to "did not","can't" to "cannot","cant" to "cannot",
            "couldn't" to "could not","couldnt" to "could not","wouldn't" to "would not","wouldnt" to "would not",
            "shouldn't" to "should not","shouldnt" to "should not","isn't" to "is not","isnt" to "is not",
            "aren't" to "are not","arent" to "are not","wasn't" to "was not","wasnt" to "was not",
            "weren't" to "were not","werent" to "were not","haven't" to "have not","havent" to "have not",
            "hasn't" to "has not","hasnt" to "has not","hadn't" to "had not","hadnt" to "had not",
            "tryna" to "trying to","hafta" to "have to","oughta" to "ought to","c'mon" to "come on")
        private val expansions=linkedMapOf("gonna" to "going to","wanna" to "want to",
            "gotta" to "have to","lemme" to "let me","gimme" to "give me",
            "dunno" to "do not know","kinda" to "kind of","sorta" to "sort of",
            "outta" to "out of","lotta" to "lot of","coulda" to "could have",
            "shoulda" to "should have","woulda" to "would have")
        private val expansionPatterns=expansions.map { (word,value) -> Regex("(?i)\\b${Regex.escape(word)}\\b") to value }
        private val dialoguePatterns=dialogueExpansions.map { (word,value) -> Regex("(?i)\\b${Regex.escape(word)}\\b") to value }
    }
}
