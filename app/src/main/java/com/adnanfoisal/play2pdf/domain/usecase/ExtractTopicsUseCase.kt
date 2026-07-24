package com.adnanfoisal.play2pdf.domain.usecase

import com.adnanfoisal.play2pdf.data.repository.CompileRepository
import javax.inject.Inject

class ExtractTopicsUseCase @Inject constructor(
    private val repo: CompileRepository
) {
    suspend operator fun invoke(playlistUrls: List<String>) =
        repo.extractTopics(playlistUrls)
}
