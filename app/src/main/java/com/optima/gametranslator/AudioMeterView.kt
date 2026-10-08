package com.optima.gametranslator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/** Static until real PCM levels arrive; no decorative animation pretending to detect sound. */
class AudioMeterView(context: Context): View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    var color=0xff57d9c0.toInt()
    var level=0
        set(value) { field=value.coerceIn(0,100);invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color=color
        val gap=width/23f;val center=height/2f
        for(i in 0 until 15) {
            val weight=1f-kotlin.math.abs(i-7)/9f
            val size=(height*.08f+height*.72f*weight*level/100f).coerceAtLeast(4f)
            val left=(width-15*gap)/2+i*gap
            paint.alpha=if(level==0)90 else 235
            canvas.drawRoundRect(left,center-size/2,left+gap*.48f,center+size/2,gap*.3f,gap*.3f,paint)
        }
    }
}
