package com.optima.gametranslator
object TranslationSourceGuard {
 private fun words(t: String)=Regex("[\\p{L}\\p{N}]+").findAll(t).map { it.value }.toList()
 private fun key(t: String)=words(t).joinToString(" ").lowercase(java.util.Locale.ROOT)
 private val connectors=mapOf("and" to "Ve","but" to "Ama","or" to "Veya","because" to "Çünkü")
 fun exact(t: String): String?=connectors[key(t)]
 fun connector(t: String)=exact(t)!=null
 fun useBackground(t: String)=words(t).size>=4
 fun validate(source: String,raw: String): String {
  val t=raw.trim();require(t.isNotBlank())
  val n=words(source).size
  if(n in 1..3)require(words(t).size<=n*2+1) { "Short source expanded beyond evidence" }
  exact(source)?.let { require(key(t)==key(it)) { "Connector meaning changed" } }
  return t
 }
}
