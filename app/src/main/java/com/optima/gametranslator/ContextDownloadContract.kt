package com.optima.gametranslator

/** Validate byte ranges before opening a resumable model file for writing. */
object ContextDownloadContract {
    data class Transfer(val offset: Long,val append: Boolean)
    fun response(status: Int,offset: Long,length: Long,contentRange: String?,total: Long): Transfer {
        require(offset in 0..total&&total>0)
        val start=when(status) {
            200 -> 0L
            206 -> {
                val match=Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(contentRange ?: "")
                require(match!=null) { "Missing content range" }
                require(match.groupValues[1].toLongOrNull()==offset&&match.groupValues[2].toLongOrNull()==total-1&&match.groupValues[3].toLongOrNull()==total) { "Invalid content range" }
                offset
            }
            else -> error("Unsupported response")
        }
        require(length<0||length==total-start) { "Unexpected model length" }
        return Transfer(start,status==206&&start>0)
    }
}
