package com.optima.gametranslator

/** Debounce revised translation previews without delaying the source transcript. */
class PartialStability {
    private var previous=""
    private var changedAt=0L
    private var pendingSince=-1L
    private var lastSubmitted=-1L
    fun observe(text: String,now: Long) { if(text!=previous) { previous=text;changedAt=now;if(pendingSince<0)pendingSince=now } }
    fun ready(text: String,now: Long)=text==previous&&text.isNotBlank()&&
        (lastSubmitted<0||now-lastSubmitted>=350)&&
        (now-changedAt>=180||(pendingSince>=0&&now-pendingSince>=450))
    fun submitted(now: Long) { pendingSince=now;lastSubmitted=now }
    fun reset() { previous="";changedAt=0;pendingSince=-1;lastSubmitted=-1 }
}
