package com.optima.gametranslator

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** Resample a streamed TTS PCM reference; chunk boundaries may split a sample. */
class ReferencePcm(private val rate: Int,private val format: Int,private val channels: Int) {
    private var carry=ByteArray(0)
    private var index=-1L
    private var next=0.0
    private var previous=0f
    fun decode(bytes: ByteArray): FloatArray {
        if(rate !in 8000..96000||channels !in 1..8)return FloatArray(0)
        val width=when(format) { 2->2;3->1;4->4;else->return FloatArray(0) }
        val data=carry+bytes;val frame=width*channels;val count=data.size/frame
        val input=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val out=FloatArray((count*16000.0/rate).toInt()+8);var used=0
        repeat(count) {
            var sum=0f
            repeat(channels) { sum+=when(format) { 2->input.short/32768f;3->((input.get().toInt() and 255)-128)/128f;else->input.float.takeIf { it.isFinite() }?:0f } }
            val sample=(sum/channels).coerceIn(-1f,1f);index++
            if(index==0L) { previous=sample }
            while(next<=index) {
                val fraction=if(index==0L)0.0 else (next-(index-1)).coerceIn(0.0,1.0)
                out[used++]=(previous+(sample-previous)*fraction).toFloat();next+=rate/16000.0
            }
            previous=sample
        }
        carry=data.copyOfRange(count*frame,data.size)
        return out.copyOf(used)
    }
}

/** Conservative reference subtraction, never a microphone mute. Hardware AEC remains device dependent. */
class EchoReference {
    private class Segment(val id: String,val registeredAt: Long) {
        var startedAt=-1L;var endedAt=-1L
        var decoder: ReferencePcm?=null
        val samples=FloatArray(16000*24);var count=0
    }
    private val segments=java.util.ArrayDeque<Segment>()
    @Synchronized fun register(id: String,now: Long) {
        trim(now);segments.addLast(Segment(id,now));while(segments.size>3)segments.removeFirst()
    }
    @Synchronized fun format(id: String,rate: Int,format: Int,channels: Int) {
        segments.firstOrNull { it.id==id }?.let { it.decoder=ReferencePcm(rate,format,channels) }
    }
    @Synchronized fun audio(id: String,bytes: ByteArray) {
        val s=segments.firstOrNull { it.id==id }?:return
        if(s.count==s.samples.size)return
        val decoded=s.decoder?.decode(bytes)?:return
        val count=minOf(decoded.size,s.samples.size-s.count)
        decoded.copyInto(s.samples,s.count,0,count);s.count+=count
    }
    @Synchronized fun started(id: String,now: Long) { segments.firstOrNull { it.id==id }?.startedAt=now }
    @Synchronized fun finished(id: String,now: Long) { segments.firstOrNull { it.id==id }?.endedAt=now }
    @Synchronized fun stopped(now: Long) { segments.forEach { if(it.endedAt<0)it.endedAt=now };trim(now) }
    private fun trim(now: Long) { segments.removeAll { now-it.registeredAt>30000||(it.endedAt>=0&&now-it.endedAt>1200) } }
    @Synchronized fun clean(mic: FloatArray,capturedEnd: Long): FloatArray {
        trim(capturedEnd)
        if(mic.isEmpty())return mic
        val micEnergy=mic.sumOf { it.toDouble()*it };if(micEnergy<1e-7)return mic
        var best=0.0;var position=0;var reference: Segment?=null
        fun score(s: Segment,offset: Int,stride: Int): Double {
            if(offset<0||offset+mic.size>s.count)return 0.0
            var dot=0.0;var a=0.0;var b=0.0
            var i=0
            while(i<mic.size) { val x=mic[i].toDouble();val y=s.samples[offset+i].toDouble();dot+=x*y;a+=x*x;b+=y*y;i+=stride }
            return if(a*b<1e-12)0.0 else abs(dot)/sqrt(a*b)
        }
        for(s in segments) {
            if(s.startedAt<0)continue
            val at=((capturedEnd-s.startedAt)*16-mic.size).toInt()
            var offset=at-12800
            while(offset<=at+1600) {
                val value=score(s,offset,8)
                if(value>best) { best=value;position=offset;reference=s }
                offset+=64
            }
        }
        val s=reference?:return mic
        val coarse=position;best=score(s,coarse,2)
        for(offset in coarse-64..coarse+64) {
            val value=score(s,offset,2)
            if(value>best) { best=value;position=offset }
        }
        // Verify full-resolution correlation; an accidental sparse match must not alter source speech.
        if(score(s,position,1)<.82)return mic
        var dot=0.0;var energy=0.0
        for(i in mic.indices) { val value=s.samples[position+i].toDouble();dot+=mic[i]*value;energy+=value*value }
        if(energy<1e-8)return mic
        val gain=(dot/energy).coerceIn(-3.0,3.0)
        return FloatArray(mic.size) { (mic[it]-gain*s.samples[position+it]).toFloat().coerceIn(-1f,1f) }
    }
}
