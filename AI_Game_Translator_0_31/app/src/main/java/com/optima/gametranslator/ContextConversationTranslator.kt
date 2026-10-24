package com.optima.gametranslator

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Optional translation-specific LLM. No audio/network; one physical native request, no inference FIFO. */
class ContextConversationTranslator(context: Context,private val pack: ContextModelPack): AutoCloseable {
    private val app=context.applicationContext
    private val cache=File(context.cacheDir,"context-mt-cache").apply { mkdirs() }
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val lock=Any()
    private val busy=AtomicBoolean(false)
    private val closed=AtomicBoolean(false)
    private val policy=ContextTranslationPolicy()
    private var engine: Engine?=null // worker only
    private var conversation: Conversation?=null // guarded by lock: cancel cannot race native delete
    private var generation=0L
    @Volatile private var initialized=false
    @Volatile private var degraded=false
    @Volatile private var calibrated=false
    @Volatile var backendName=""
        private set
    val ready: Boolean get()=initialized&&calibrated&&!degraded&&!closed.get()
    fun eligible(): Boolean {
        val memory=ActivityManager.MemoryInfo()
        (app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        return ContextTranslationPolicy.eligible(Process.is64Bit(),memory.totalMem,memory.availMem,memory.lowMemory)
    }
    fun prepare(): Task<String> {
        if(ready)return Tasks.forResult(backendName)
        if(!initialized&&!eligible())return Tasks.forException(IllegalStateException("Context memory/ABI requirement"))
        return submit(90_000) { ticket ->
            if(!initialized) {
                runCatching { engine?.close() };engine=null
                pack.verify();checkCurrent(ticket);cache.mkdirs()
                check(cache.usableSpace>=1024L*1024*1024) { "Insufficient model cache storage" }
                var created: Engine?=null
                try {
                    created=Engine(EngineConfig(pack.file.absolutePath,backend=Backend.GPU(),maxNumTokens=1024,cacheDir=cache.absolutePath))
                    created.initialize();backendName="GPU"
                } catch(error: Exception) {
                    runCatching { created?.close() }
                    checkCurrent(ticket)
                    created=Engine(EngineConfig(pack.file.absolutePath,backend=Backend.CPU(2),maxNumTokens=1024,cacheDir=cache.absolutePath))
                    created.initialize();backendName="CPU"
                }
                engine=created;initialized=true
            }
            checkCurrent(ticket)
            val start=SystemClock.elapsedRealtime()
            // Warm up before microphone capture or trial accounting; this text never enters user history/context.
            infer("Keep moving. We are running out of time.",ticket,false)
            if(SystemClock.elapsedRealtime()-start>3500) { degraded=true;throw IllegalStateException("Context backend too slow for live use") }
            checkCurrent(ticket);calibrated=true;degraded=false;backendName
        }
    }
    fun translate(source: String): Task<String> {
        if(!ready)return Tasks.forException(IllegalStateException("Context backend unavailable"))
        return submit(4500) { ticket -> infer(source,ticket,true) }
    }
    private fun checkCurrent(ticket: Long)=synchronized(lock) {
        check(!closed.get()&&generation==ticket) { "Context session changed" }
    }
    private fun infer(source: String,ticket: Long,remember: Boolean): String {
        checkCurrent(ticket)
        val prompt=policy.prompt(source,SystemClock.elapsedRealtime())
        // This model's template supports one message per Conversation. Never append a second message.
        // Only the last two accepted English sources are embedded as background in a NEW conversation.
        val current=checkNotNull(engine).createConversation(ConversationConfig(
            samplerConfig=SamplerConfig(1,1.0,0.0),automaticToolCalling=false,maxOutputToken=160))
        try {
            synchronized(lock) { checkCurrent(ticket);conversation=current }
            val response=current.sendMessage(prompt)
            check(response.toolCalls.isEmpty()&&response.channels.isEmpty()) { "Unexpected model response" }
            val text=response.contents.contents.map { part -> check(part is Content.Text);part.text }.joinToString("")
            val validated=ContextTranslationPolicy.validate(source,text)
            synchronized(lock) {
                checkCurrent(ticket)
                if(remember)policy.accepted(source,SystemClock.elapsedRealtime())
            }
            return validated
        } finally {
            synchronized(lock) { if(conversation===current)conversation=null;current.close() }
        }
    }
    private fun submit(timeoutMs: Long,operation: (Long)->String): Task<String> {
        val completion=TaskCompletionSource<String>()
        if(closed.get()||!busy.compareAndSet(false,true)) {
            completion.setException(IllegalStateException("Context native worker occupied"));return completion.task
        }
        val ticket=synchronized(lock) { generation }
        val finished=AtomicBoolean(false)
        val deadline=Runnable {
            if(!finished.compareAndSet(false,true))return@Runnable
            synchronized(lock) {
                if(generation==ticket) {
                    degraded=true;generation++;policy.clear();runCatching { conversation?.cancelProcess() }
                }
            }
            completion.trySetException(IllegalStateException("Context deadline; fast translation required"))
        }
        main.postDelayed(deadline,timeoutMs)
        worker.execute {
            try {
                val value=operation(ticket)
                synchronized(lock) {
                    checkCurrent(ticket)
                    if(finished.compareAndSet(false,true))completion.trySetResult(value)
                }
            } catch(error: Exception) { if(finished.compareAndSet(false,true))completion.trySetException(error) }
            catch(_: LinkageError) { initialized=false;calibrated=false;degraded=true;if(finished.compareAndSet(false,true))completion.trySetException(IllegalStateException("Context native backend unavailable")) }
            catch(_: OutOfMemoryError) { initialized=false;calibrated=false;degraded=true;runCatching { engine?.close() };engine=null;if(finished.compareAndSet(false,true))completion.trySetException(IllegalStateException("Context memory limit")) }
            finally { main.removeCallbacks(deadline);busy.set(false) }
        }
        return completion.task
    }
    /** Invalidate context/results before stopping capture. Cancel runs while the native pointer is alive. */
    fun reset() {
        synchronized(lock) { generation++;policy.clear();runCatching { conversation?.cancelProcess() } }
        // Keep slow-backend degradation for this engine instance; do not retry every clause.
    }
    /** Release model memory off the UI thread when leaving the app or deleting the pack. */
    fun unload(clearCache: Boolean=false) {
        reset();initialized=false;calibrated=false
        if(!closed.get())worker.execute { runCatching { engine?.close() };engine=null;initialized=false;calibrated=false;backendName="";degraded=false;if(clearCache)cache.listFiles()?.forEach { it.deleteRecursively() } }
    }
    override fun close() {
        if(!closed.compareAndSet(false,true))return
        reset();initialized=false;calibrated=false
        worker.execute { runCatching { engine?.close() };engine=null };worker.shutdown()
    }
}
