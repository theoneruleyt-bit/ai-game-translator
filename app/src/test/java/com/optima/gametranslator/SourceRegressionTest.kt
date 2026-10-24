package com.optima.gametranslator
import org.junit.Test
class SourceRegressionTest {
@Test fun sourceAndConnectorRegression() {
 check(TranslationSourceGuard.exact("AND.")=="Ve")
 check(runCatching { TranslationSourceGuard.validate("And","Ve bir devlet bile isteksiz olamaz.") }.isFailure)
 val p=ContextTranslationPolicy();p.accepted("Even a state cannot be unwilling.",0)
 check(!p.prompt("And",1).contains("Even a state"))
 check(p.prompt("We need to leave now.",1).contains(ContextTranslationPolicy.INSTRUCTION))
 val t=StreamingTurns();check(!t.update("And.",true,0).single().final)
 check(t.update("AND",true,100).isEmpty())
 check(t.tick(650).single().text.isEmpty());check(t.tick(2000).isEmpty())
 val c=StreamingTurns();c.update("And",true,0)
 check(c.update("we need to leave now.",true,200).single().text=="And we need to leave now.")
 check(c.update("Go!",true,1000).single().text=="Go!")
 check(c.update("Go!",true,1100).single().text=="Go!")
 check(ContextTranslationPolicy.validate("Duck!","Siper al!")=="Siper al!")
 println("12 targeted Kotlin regression checks PASS")
}

}
