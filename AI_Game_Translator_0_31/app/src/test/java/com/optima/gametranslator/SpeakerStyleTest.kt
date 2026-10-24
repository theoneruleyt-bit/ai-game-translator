package com.optima.gametranslator

import org.junit.Assert.*
import org.junit.Test

class SpeakerStyleTest {
    @Test fun queueAccelerationIsBoundedAndOptional() {
        assertEquals(1.1f,SpeakerStyle.catchUpRate(1.1f,8,false),.001f)
        assertEquals(1.1f,SpeakerStyle.catchUpRate(1.1f,0,true),.001f)
        assertTrue(SpeakerStyle.catchUpRate(1.1f,4,true)>1.1f)
        assertEquals(1.65f,SpeakerStyle.catchUpRate(1.5f,8,true),.001f)
    }
    @Test fun routerSupportsSixteenDistinctNames() {
        val router=SpeakerRouter()
        val names=listOf("Alice","Bob","Carol","David","Emma","Frank","Grace","Helen","Ian","Jane","Karl","Lucy","Mike","Nora","Oscar","Peter")
        names.forEachIndexed { index,name -> assertEquals(index+1,router.route("$name: Hello",0,true).single().slot) }
        assertEquals(0,router.route("Quinn: Hello",0,true).single().slot)
    }
}
