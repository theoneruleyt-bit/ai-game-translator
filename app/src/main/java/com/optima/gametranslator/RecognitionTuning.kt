package com.optima.gametranslator

import java.io.File

/** Kaldi endpoint rules are expressed in seconds. Keep an original config for repeatable changes. */
object RecognitionTuning {
    fun configure(model: File,mode: Int) {
        val config=File(model,"conf/model.conf")
        val original=File(model,"conf/model.conf.original")
        if(!original.exists()) config.copyTo(original)
        val values=when(mode) {
            0 -> floatArrayOf(.20f,.40f,.65f,3f)
            1 -> floatArrayOf(.30f,.60f,.90f,5f)
            else -> floatArrayOf(.45f,.80f,1.20f,8f)
        }
        var text=original.readText()
        val options=linkedMapOf("endpoint.rule2.min-trailing-silence" to values[0],
            "endpoint.rule3.min-trailing-silence" to values[1],"endpoint.rule4.min-trailing-silence" to values[2],
            "endpoint.rule5.min-utterance-length" to values[3])
        for((name,value) in options) {
            text=text.replace(Regex("(?m)^\\s*--"+Regex.escape(name)+"(?:=|\\s+)[^\\r\\n]*$"),"")
            text=text.trimEnd()+"\n--$name=$value\n"
        }
        config.writeText(text)
    }
}
