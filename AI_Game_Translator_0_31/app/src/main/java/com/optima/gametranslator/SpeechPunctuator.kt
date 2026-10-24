package com.optima.gametranslator

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.OnlinePunctuation
import com.k2fsa.sherpa.onnx.OnlinePunctuationConfig
import com.k2fsa.sherpa.onnx.OnlinePunctuationModelConfig
import java.security.MessageDigest

/** Small English CNN-BiLSTM punctuation model, bundled and verified before native loading. */
class SpeechPunctuator(assets: AssetManager): AutoCloseable {
    private val engine: OnlinePunctuation
    init {
        for((name,sha) in mapOf("model.int8.onnx" to "9d611f445fe4a46186080fe161be6059d87d72eb88d3a8cb00c1a06e83a6067e",
            "bpe.vocab" to "e118b7ad88c54db562517df49e1cffd4836d166c34fb190fd311d7f34eb238f5")) {
            val digest=MessageDigest.getInstance("SHA-256")
            assets.open("punctuation/$name").use { input ->
                val data=ByteArray(65536)
                while(true) { val n=input.read(data);if(n<0)break;digest.update(data,0,n) }
            }
            check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }==sha)
        }
        engine=OnlinePunctuation(assets,OnlinePunctuationConfig(model=OnlinePunctuationModelConfig(
            cnnBilstm="punctuation/model.int8.onnx",bpeVocab="punctuation/bpe.vocab",numThreads=1,provider="cpu")))
    }
    fun apply(text: String)=engine.addPunctuation(PunctuationInput.prepare(text))
    override fun close() { engine.release() }
}
