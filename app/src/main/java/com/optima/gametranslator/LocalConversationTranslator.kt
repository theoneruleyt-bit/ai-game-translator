package com.optima.gametranslator

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Optional independent OPUS-MT engine. One physical request; no truncated output is presented. */
class LocalConversationTranslator(context: Context): AutoCloseable {
    private val assets=context.assets
    private val worker=Executors.newSingleThreadExecutor()
    private val busy=AtomicBoolean(false)
    private val closed=AtomicBoolean(false)
    private val main=Handler(Looper.getMainLooper())
    private var engine: Interpreter?=null
    private var model: ByteBuffer?=null
    private var tokenizer: EnglishUnigram?=null
    private fun load() {
        if(engine!=null)return
        val bytes=assets.open("translation-en-tr/model.tflite").use { it.readBytes() }
        val sha=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        check(sha=="6bab379f1f20f8ba927a1494603dcaca25e1882d5f83c2efecfb71060d7ac723")
        val buffer=ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply { put(bytes);rewind() }
        val created=Interpreter(buffer,Interpreter.Options().setNumThreads(2))
        try {
            val pieceBytes=assets.open("translation-en-tr/pieces.tsv").use { it.readBytes() }
            check(MessageDigest.getInstance("SHA-256").digest(pieceBytes).joinToString("") { "%02x".format(it.toInt() and 255) }=="bb621d6effa7923e159c2238b8e9f7d0be789b9721a7d7c6365efe9ac3f07913")
            tokenizer=EnglishUnigram(pieceBytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.toList())
            check(created.signatureKeys.toSet()==setOf("encode","prefill","decode"))
            model=buffer;engine=created
        } catch(error: Throwable) { created.close();throw error }
    }
    private fun buffer(bytes: Int)=ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
    private fun infer(text: String): String {
        load();val e=checkNotNull(engine);val tok=checkNotNull(tokenizer)
        val ids=tok.encode(text);require(ids.size<=256&&ids.none { it==1 }) { "Input requires fallback" }
        val start=android.os.SystemClock.elapsedRealtime()
        fun checkDeadline() { check(!closed.get()&&android.os.SystemClock.elapsedRealtime()-start<2500) { "Local translation deadline" } }
        val tokens=buffer(256*4);for(i in 0 until 256)tokens.putInt(if(i<ids.size)ids[i] else 0);tokens.rewind()
        val positions=buffer(256*4);repeat(256) { positions.putInt(it) };positions.rewind()
        val mask=buffer(256*4);repeat(256) { mask.putFloat(if(it<ids.size)0f else -1e9f) };mask.rewind()
        val hidden=buffer(e.getOutputTensorFromSignature("output_0","encode").numBytes())
        e.runSignature(mapOf("input_ids" to tokens,"input_pos" to positions,"pad_mask" to mask),mapOf("output_0" to hidden),"encode")
        checkDeadline()
        val cacheNames=e.getSignatureInputs("prefill").filter { "cache" in it }
        var caches=cacheNames.associateWith { buffer(e.getInputTensorFromSignature(it,"prefill").numBytes()) }
        var spare=cacheNames.associateWith { buffer(e.getOutputTensorFromSignature(it,"prefill").numBytes()) }
        val logits=buffer(32001*4);val nextId=buffer(4);val nextPos=buffer(4)
        val output=mutableListOf<Int>();var token=32000
        for(step in 0 until 128) {
            checkDeadline();val signature=if(step==0)"prefill" else "decode"
            nextId.clear();nextId.putInt(token);nextId.rewind();nextPos.clear();nextPos.putInt(step);nextPos.rewind()
            hidden.rewind();mask.rewind();logits.clear()
            val inputs=mutableMapOf<String,Any>("decoder_input_ids" to nextId,"decoder_input_pos" to nextPos,"encoder_hidden_states" to hidden,"pad_mask" to mask)
            caches.forEach { (name,value) -> value.rewind();inputs[name]=value }
            val outputs=mutableMapOf<String,Any>("logits" to logits)
            spare.forEach { (name,value) -> value.clear();outputs[name]=value }
            e.runSignature(inputs,outputs,signature)
            logits.rewind();var best=Float.NEGATIVE_INFINITY;token=0
            for(id in 0 until 32001) { val score=logits.float;if(id!=32000&&score.isFinite()&&score>best) { best=score;token=id } }
            check(best.isFinite())
            val old=caches;caches=spare;spare=old
            if(token==0) { checkDeadline();return tok.decode(output).also { check(it.isNotBlank()) } }
            output.add(token)
        }
        error("No end token; refusing truncated translation")
    }
    fun translate(text: String): Task<String> {
        val result=TaskCompletionSource<String>()
        if(closed.get()||!busy.compareAndSet(false,true)) {
            result.setException(IllegalStateException("Local translator busy"));return result.task
        }
        worker.execute {
            try { val translated=infer(text);main.post { result.trySetResult(translated) } }
            catch(error: Exception) { main.post { result.trySetException(error) } }
            catch(_: LinkageError) { main.post { result.trySetException(IllegalStateException("Local runtime unavailable")) } }
            catch(_: OutOfMemoryError) { engine?.close();engine=null;model=null;tokenizer=null;main.post { result.trySetException(IllegalStateException("Local runtime memory limit")) } }
            finally { busy.set(false) }
        }
        return result.task
    }
    override fun close() {
        if(!closed.compareAndSet(false,true))return
        worker.execute { engine?.close();engine=null;model=null;tokenizer=null };worker.shutdown()
    }
}
