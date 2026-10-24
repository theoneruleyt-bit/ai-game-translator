package com.optima.gametranslator

/** Immutable, checksum-pinned on-device models. Downloads have no inference API charge. */
data class LocalSpeechModel(val id: String,val folder: String,val base: String,val parts: List<SpeechModelPart>) {
    val downloadBytes: Long get()=parts.sumOf { it.size }
}
data class SpeechModelPart(val file: String,val size: Long,val sha: String)
object LocalSpeechModels {
    val giga=LocalSpeechModel("giga","free-streaming-en-giga-v1",
        "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-21/resolve/9a65b6ea94c311ca770c2bf895b30f456a22d703/",listOf(
        SpeechModelPart("encoder-epoch-99-avg-1.int8.onnx",187823992L,"32c98281c7bd8b63e3e142d007251b37f120572e8fdea9a4f5a79ce22b10ec4f"),
        SpeechModelPart("decoder-epoch-99-avg-1.onnx",2092566L,"9da02b77cb08826756ec6a88635f35a40374e4164e7c6359121a9145958a6ceb"),
        SpeechModelPart("joiner-epoch-99-avg-1.int8.onnx",259335L,"831477d390e59a61f1b6a6f763b9903e6c6366ff6034f1ddba613be82637122f"),
        SpeechModelPart("tokens.txt",5048L,"49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb")))
    val light=LocalSpeechModel("light","free-streaming-en-v1",
        "https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/resolve/672fbf1b30579d6585301139bb363f42a0ad4a24/",listOf(
        SpeechModelPart("encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx",71082637L,"0d072fd4ef956294ba9db9e9a71a541ac70659095ec4934c8453d8b2fe740187"),
        SpeechModelPart("decoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx",1307236L,"98da299f471e38bb4e1a8df579b8cc9122d6039576a77e357b3c60f17dd83b02"),
        SpeechModelPart("joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx",259335L,"d944208d660d67c8d72cd2acaeac971fa5ceb8c80e76c1968148846fedd6e297"),
        SpeechModelPart("tokens.txt",5048L,"49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb")))
    val all=listOf(giga,light)
    fun forId(id: String?)=all.firstOrNull { it.id==id }?:giga
}
