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

    /**
     * Total videos across the selected playlists, when playlist metadata was
     * fetched successfully. Null when unknown — the History card then falls
     * back to a neutral label instead of mislabelling the playlist count as
     * a video count.
     */
    var videoCount: Int? = null
}
