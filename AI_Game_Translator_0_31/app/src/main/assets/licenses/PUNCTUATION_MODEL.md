# Bundled English punctuation model

- Model: sherpa-onnx-online-punct-en-2024-08-06, CNN-BiLSTM, int8.
- Author: Jian You / frankyoujian, Edge-Punct-Casing.
- Upstream project: https://github.com/frankyoujian/Edge-Punct-Casing
- Upstream model card: https://huggingface.co/frankyoujian/Edge-Punct-Casing/tree/bde453bde24b544f38ffdd56398bfadcd22ce935
- License: Apache-2.0 (upstream model card and source LICENSE). Included as Edge-Punct-Casing-APACHE-2.0.txt.
- Converted model distribution: https://github.com/k2-fsa/sherpa-onnx/releases/download/punctuation-models/sherpa-onnx-online-punct-en-2024-08-06.tar.bz2
- Sherpa documentation: https://k2-fsa.github.io/sherpa/onnx/punctuation/pretrained_models.html

Bundled assets under app/src/main/assets/punctuation:

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| model.int8.onnx | 7490500 | 9d611f445fe4a46186080fe161be6059d87d72eb88d3a8cb00c1a06e83a6067e |
| bpe.vocab | 149430 | e118b7ad88c54db562517df49e1cffd4836d166c34fb190fd311d7f34eb238f5 |

Assets are used unchanged from the model distribution. This application adds a separate conservative segmentation policy; that policy is not part of the upstream model. No audio or transcript is sent to a server by this component.
