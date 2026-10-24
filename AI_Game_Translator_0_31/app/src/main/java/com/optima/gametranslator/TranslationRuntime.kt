package com.optima.gametranslator

import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.*

/** Separate final/preview clients. Bounded native requests and bounded UI waiting. Main thread only. */
class TranslationRuntime {
    class Busy: IllegalStateException("Translation engine is still processing")
    class Deadline: IllegalStateException("Translation timed out")
    private val main=Handler(Looper.getMainLooper())
    private val budget=RequestBudget()
    private val clients=mutableMapOf<Pair<Boolean,Boolean>,Translator>()
    private val pending=mutableMapOf<Long,Pair<TaskCompletionSource<String>,Runnable>>()
    private var closed=false
    private fun client(reverse: Boolean,preview: Boolean)=clients.getOrPut(reverse to preview) {
        Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(if(reverse) TranslateLanguage.TURKISH else TranslateLanguage.ENGLISH)
            .setTargetLanguage(if(reverse) TranslateLanguage.ENGLISH else TranslateLanguage.TURKISH).build())
    }
    fun prepare(): Task<Void> = Tasks.whenAll(
        client(false,false).downloadModelIfNeeded(),client(true,false).downloadModelIfNeeded(),
        client(false,true).downloadModelIfNeeded(),client(true,true).downloadModelIfNeeded())
    fun translate(text: String,reverse: Boolean,preview: Boolean): Task<String> {
        if(closed)return Tasks.forException(IllegalStateException("Translation closed"))
        val id=budget.acquire(if(preview) RequestBudget.Lane.PREVIEW else RequestBudget.Lane.FINAL)
            ?: return Tasks.forException(Busy())
        val output=TaskCompletionSource<String>()
        val timeout=Runnable { pending.remove(id);output.trySetException(Deadline()) }
        pending[id]=output to timeout;main.postDelayed(timeout,if(preview)2000 else 12000)
        try {
            client(reverse,preview).translate(text).addOnSuccessListener { output.trySetResult(it) }
                .addOnFailureListener { output.trySetException(it) }
                .addOnCompleteListener {
                    main.removeCallbacks(timeout);pending.remove(id);budget.release(id)
                    if(it.isCanceled)output.trySetException(IllegalStateException("Translation canceled"))
                }
        } catch(error: Exception) {
            main.removeCallbacks(timeout);pending.remove(id);budget.release(id);output.trySetException(error)
        }
        return output.task
    }
    fun close() {
        closed=true
        pending.values.toList().forEach { (output,timer) -> main.removeCallbacks(timer);output.trySetException(IllegalStateException("Translation closed")) }
        pending.clear();clients.values.forEach { it.close() };clients.clear()
    }
}
