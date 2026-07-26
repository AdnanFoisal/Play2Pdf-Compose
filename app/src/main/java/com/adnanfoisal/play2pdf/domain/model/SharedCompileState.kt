package com.adnanfoisal.play2pdf.domain.model

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedCompileState @Inject constructor() {
    var subject: String = ""
    var author: String = ""
    var playlistUrls: List<String> = emptyList()
    var topics: List<String> = emptyList()
    var theme: PdfTheme = PdfTheme.NordicFrost
}
