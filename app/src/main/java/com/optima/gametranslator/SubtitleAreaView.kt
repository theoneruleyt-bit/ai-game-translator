package com.optima.gametranslator

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

/** Draw a new subtitle rectangle with one drag; coordinates are preview-local. */
class SubtitleAreaView(context: Context): View(context) {
    private val area=RectF(.10f,.55f,.90f,.78f)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var anchorX=0f
    private var anchorY=0f
    var onAreaChanged: (() -> Unit)?=null
    init { contentDescription="Altyazı okuma alanı. Yeni bir alan çizmek için sürükle." }
    fun bounds()=RectF(area.left*width,area.top*height,area.right*width,area.bottom*height)
    fun resetArea() { area.set(.10f,.55f,.90f,.78f);invalidate();onAreaChanged?.invoke() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r=bounds()
        paint.style=Paint.Style.FILL;paint.color=0x66101927
        canvas.drawRect(0f,0f,width.toFloat(),r.top,paint)
        canvas.drawRect(0f,r.bottom,width.toFloat(),height.toFloat(),paint)
        canvas.drawRect(0f,r.top,r.left,r.bottom,paint)
        canvas.drawRect(r.right,r.top,width.toFloat(),r.bottom,paint)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=3*resources.displayMetrics.density;paint.color=Color.rgb(100,235,204)
        canvas.drawRoundRect(r,10f,10f,paint)
        paint.style=Paint.Style.FILL;paint.textSize=12*resources.displayMetrics.scaledDensity
        canvas.drawText("Altyazı alanı • sürükleyerek seç",12f,20*resources.displayMetrics.density,paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(width==0||height==0)return false
        val x=(event.x/width).coerceIn(0f,1f);val y=(event.y/height).coerceIn(0f,1f)
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN->{ parent.requestDisallowInterceptTouchEvent(true);anchorX=x;anchorY=y;return true }
            MotionEvent.ACTION_MOVE->{
                area.set(minOf(anchorX,x),minOf(anchorY,y),maxOf(anchorX,x),maxOf(anchorY,y));invalidate();return true
            }
            MotionEvent.ACTION_UP->{
                parent.requestDisallowInterceptTouchEvent(false)
                if(area.width()<.08f||area.height()<.06f) area.set(.10f,.55f,.90f,.78f)
                invalidate();performClick();onAreaChanged?.invoke();return true
            }
            MotionEvent.ACTION_CANCEL->{parent.requestDisallowInterceptTouchEvent(false);return true}
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick();return true }
}
