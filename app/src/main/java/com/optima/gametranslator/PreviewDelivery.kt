package com.optima.gametranslator

/** Drafts are revisable. A tail rewrite must not hide every completed translation in a live stream. */
object PreviewDelivery {
    fun allows(sameUtterance: Boolean,revision: Long,lastDisplayed: Long,finalPreview: Boolean,
               currentFinal: Boolean,currentText: String): Boolean =
        sameUtterance && revision>=lastDisplayed && finalPreview==currentFinal && currentText.isNotBlank()
}
