# Optional English → Turkish OPUS-MT model

Model author/distributor: Helsinki-NLP. Model: opus-mt_tiny_eng-tur, distilled using OpusDistillery from the OPUS-MT Tatoeba teacher. Upstream declares Apache-2.0. Standard license text is included in OPUS_MT_APACHE-2.0.txt.

Pinned upstream: https://huggingface.co/Helsinki-NLP/opus-mt_tiny_eng-tur/tree/daadff27d04f77a1f37b43ab4983ee50468ea7d7

Bundled unchanged model.tflite (20144264 bytes): SHA-256 6bab379f1f20f8ba927a1494603dcaca25e1882d5f83c2efecfb71060d7ac723.

pieces.tsv is a derived table of token strings, original unigram float scores and Marian vocabulary IDs from the pinned source.spm and vocab.json:

- source.spm SHA-256: fe55e84b94d17b424eafba19d1e4815a90b23cd52138b9cdb1072d1b65c9795c
- vocab.json SHA-256: 5ad08bcc77230030648df7079b1523db693030fb9a206d83effbc66f2944f268
- derived pieces.tsv SHA-256: bb621d6effa7923e159c2238b8e9f7d0be789b9721a7d7c6365efe9ac3f07913

The narrow ASCII-English Kotlin adapter and Android inference orchestration are application code. The adapter deliberately rejects unsupported Unicode and the app falls back to ML Kit; it is not a full replacement for the general SentencePiece normalizer. On the supported source domain it is regression-tested against 684 native SentencePiece encodings. Turkish output decoding preserves Unicode.

Runtime: Google LiteRT 1.4.1, Apache-2.0, via the pinned Google Maven dependency. Model inputs and signatures were inspected directly. No transcript is uploaded by this engine.

The authored conversation pack is a separate exact-phrase resource, not a newly trained neural translation model. No claim is made that this optional OPUS-MT model universally outperforms ML Kit or covers all slang.
