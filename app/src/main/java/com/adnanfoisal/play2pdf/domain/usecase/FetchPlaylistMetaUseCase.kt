package com.adnanfoisal.play2pdf.domain.usecase

import com.adnanfoisal.play2pdf.data.repository.CompileRepository
import javax.inject.Inject

class FetchPlaylistMetaUseCase @Inject constructor(
    private val repo: CompileRepository
) {
    suspend operator fun invoke(playlistUrl: String) = repo.fetchPlaylistMeta(playlistUrl)
}
