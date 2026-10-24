package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt
import kotlin.random.Random

class EchoReferenceTest {
    private fun pcm(a: FloatArray)=ByteBuffer.allocate(a.size*2).order(ByteOrder.LITTLE_ENDIAN).apply { a.forEach { putShort((it*30000).toInt().toShort()) } }.array()
    private fun rms(a: FloatArray)=sqrt(a.sumOf { it.toDouble()*it }/a.size)
    @Test fun referenceEchoIsRemovedWithoutMutingOtherSpeech() {
        val ref=FloatArray(16000) { Random(it+31).nextFloat()*.8f-.4f }
        val e=EchoReference();e.register("a",1000);e.format("a",16000,2,1);e.audio("a",pcm(ref));e.started("a",1000)
        // Playback reference at 1000 ms, echo delayed by 100 ms, mic chunk ends at 1400 ms.
        val offset=4000
        val other=FloatArray(800) { Random(it+999).nextFloat()*.04f-.02f }
        val mic=FloatArray(800) { ref[offset+it]*(30000f/32768f)*.6f+other[it] }
        val clean=e.clean(mic,1400)
        assertTrue(rms(clean)<rms(mic)*.3)
        assertTrue(rms(FloatArray(800) { clean[it]-other[it] })<.01)
    }
    @Test fun unrelatedSourceAudioIsPreserved() {
        val ref=FloatArray(16000) { Random(it).nextFloat()*.5f-.25f }
        val mic=FloatArray(800) { Random(it+20000).nextFloat()*.5f-.25f }
        val e=EchoReference();e.register("a",1000);e.format("a",16000,2,1);e.audio("a",pcm(ref));e.started("a",1000)
        assertArrayEquals(mic,e.clean(mic,1400),0f)
    }
    @Test fun oldPlaybackCannotAffectNewAudioAfterTailExpires() {
        val a=FloatArray(800) { .2f }
        val e=EchoReference();e.register("old",1000);e.format("old",16000,2,1);e.audio("old",pcm(a));e.started("old",1000);e.finished("old",1100)
        assertArrayEquals(a,e.clean(a,3000),0f)
    }
    @Test fun pcmDecoderPreservesSampleAndStereoBoundariesAcrossByteChunks() {
        val a=byteArrayOf(0,64,0,64,0,0,0,0)
        val d=ReferencePcm(16000,2,2)
        assertEquals(0,d.decode(a.copyOfRange(0,3)).size)
        val out=d.decode(a.copyOfRange(3,a.size))
        assertArrayEquals(floatArrayOf(.5f,0f),out,0.0001f)
    }
    @Test fun resamplingIsIndependentOfCallbackChunkSizes() {
        val bytes=pcm(FloatArray(2400) { it/5000f })
        val whole=ReferencePcm(24000,2,1).decode(bytes)
        val d=ReferencePcm(24000,2,1);val pieces=mutableListOf<Float>()
        for(start in bytes.indices step 37)pieces.addAll(d.decode(bytes.copyOfRange(start,minOf(bytes.size,start+37))).toList())
        assertArrayEquals(whole,pieces.toFloatArray(),0.0001f)
    }
    @Test fun unknownPcmEncodingFailsOpenWithoutAlteringMicrophone() {
        val e=EchoReference();e.register("a",0);e.format("a",16000,999,1);e.audio("a",ByteArray(100));e.started("a",0)
        val mic=floatArrayOf(.1f,.2f);assertArrayEquals(mic,e.clean(mic,100),0f)
    }
}
