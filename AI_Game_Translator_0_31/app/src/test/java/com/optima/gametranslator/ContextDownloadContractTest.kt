package com.optima.gametranslator
import org.junit.Assert.*
import org.junit.Test

class ContextDownloadContractTest {
    private val total=1_827_026_224L
    @Test fun resumesLargeFileFromExactBytePosition() {
        val offset=1_100_000_000L
        assertEquals(ContextDownloadContract.Transfer(offset,true),ContextDownloadContract.response(206,offset,total-offset,"bytes $offset-${total-1}/$total",total))
    }
    @Test fun serverIgnoringRangeMustTruncateAndRestart() {
        assertEquals(ContextDownloadContract.Transfer(0,false),ContextDownloadContract.response(200,100,total,null,total))
    }
    @Test fun refusesWrongPositionAndWrongFileBeforeOpeningOutput() {
        for(range in listOf(null,"bytes 0-${total-1}/$total","bytes 100-999/1000","bytes 100-${total-1}/*")) {
            assertThrows(IllegalArgumentException::class.java) { ContextDownloadContract.response(206,100,-1,range,total) }
        }
    }
    @Test fun refusesTruncatedResponseAndAcceptsUnknownTransportLength() {
        assertThrows(IllegalArgumentException::class.java) { ContextDownloadContract.response(200,0,total-1,null,total) }
        assertEquals(ContextDownloadContract.Transfer(0,false),ContextDownloadContract.response(200,0,-1,null,total))
    }
    @Test fun refusesOutOfBoundsLocalPartAndErrorStatus() {
        assertThrows(IllegalArgumentException::class.java) { ContextDownloadContract.response(200,total+1,total,null,total) }
        assertThrows(IllegalStateException::class.java) { ContextDownloadContract.response(416,100,0,null,total) }
    }
}
