package com.optima.gametranslator

/** Keep punctuation boundaries where possible; never split a UTF-16 surrogate pair. */
object SpeechText {
    fun split(text: String,limit: Int): List<String> {
        require(limit>=2)
        var rest=text.trim();val result=mutableListOf<String>()
        while(rest.length>limit) {
            var end=limit
            if(Character.isHighSurrogate(rest[end-1])&&Character.isLowSurrogate(rest[end]))end--
            val punctuation=(end-1 downTo end/2).firstOrNull { rest[it] in ".!?;"&&(it+1>=rest.length||rest[it+1].isWhitespace()) }
            val space=(end-1 downTo end/2).firstOrNull { rest[it].isWhitespace() }
            end=punctuation?.plus(1) ?: space ?: end
            result.add(rest.substring(0,end).trim());rest=rest.substring(end).trimStart()
        }
        if(rest.isNotBlank())result.add(rest)
        return result
    }
}
