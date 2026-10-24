package com.optima.gametranslator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PhraseBook(context: Context) {
    data class Phrase(val source: String,val translation: String)
    private val prefs=context.getSharedPreferences("saved_phrases",Context.MODE_PRIVATE)
    fun all(): List<Phrase> = try {
        val data=JSONArray(prefs.getString("items","[]"))
        (0 until minOf(data.length(),100)).mapNotNull { index ->
            val item=data.optJSONObject(index)?:return@mapNotNull null
            val from=item.optString("source");val to=item.optString("translation")
            if(to.isBlank())null else Phrase(from,to)
        }
    } catch (_: Exception) { emptyList() }
    fun add(phrase: Phrase): Boolean {
        if(phrase.translation.isBlank())return false
        val items=all().toMutableList()
        if(phrase in items)return false
        items.add(0,phrase)
        val array=JSONArray()
        items.take(100).forEach { array.put(JSONObject().put("source",it.source).put("translation",it.translation)) }
        prefs.edit().putString("items",array.toString()).apply();return true
    }
    fun clear() { prefs.edit().remove("items").apply() }
}
