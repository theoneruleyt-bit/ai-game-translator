package com.optima.gametranslator

/** Explicit model selection: larger models improve coverage but can be slower. */
data class SpeechPack(val root: String,val language: String,val archiveLimit: Long,val expandedLimit: Long) {
    companion object {
        val FAST=SpeechPack("vosk-model-small-en-us-0.15","en",100L*1024*1024,256L*1024*1024)
        val ENGLISH=SpeechPack("vosk-model-en-us-0.22-lgraph","en",200L*1024*1024,512L*1024*1024)
        val TURKISH=SpeechPack("vosk-model-small-tr-0.3","tr",100L*1024*1024,256L*1024*1024)
        val ALL=listOf(FAST,ENGLISH,TURKISH)
    }
}
