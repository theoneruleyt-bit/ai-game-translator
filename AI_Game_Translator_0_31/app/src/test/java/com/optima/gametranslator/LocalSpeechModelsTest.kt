package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test
class LocalSpeechModelsTest {
 @Test fun oldDownloadsRemainUsableWhenChoosingLight() {
  assertEquals("free-streaming-en-v1",LocalSpeechModels.forId("light").folder)
  assertEquals(72654256L,LocalSpeechModels.light.downloadBytes)
  assertNotEquals(LocalSpeechModels.giga.folder,LocalSpeechModels.light.folder)
 }
 @Test fun advancedSelectionUsesItsOwnPinnedFiles() {
  val m=LocalSpeechModels.forId(null);assertEquals("giga",m.id)
  assertEquals(m,LocalSpeechModels.forId("unknown"));assertEquals(190180941L,m.downloadBytes)
  assertTrue(m.parts[1].file.endsWith("avg-1.onnx"))
  LocalSpeechModels.all.forEach { model ->
   assertTrue(model.base.startsWith("https://"));assertTrue(model.base.matches(Regex(".*/resolve/[a-f0-9]{40}/")))
   assertEquals(4,model.parts.size)
   model.parts.forEach { assertTrue(it.sha.matches(Regex("[a-f0-9]{64}")));assertFalse(it.file.contains('/'));assertTrue(it.size>0) }
  }
 }
}
