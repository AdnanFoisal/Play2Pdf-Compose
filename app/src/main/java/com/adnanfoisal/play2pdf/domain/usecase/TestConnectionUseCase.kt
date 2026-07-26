package com.adnanfoisal.play2pdf.domain.usecase

import com.adnanfoisal.play2pdf.data.repository.ConnectionRepository
import javax.inject.Inject

class TestConnectionUseCase @Inject constructor(
    private val repo: ConnectionRepository
) {
    suspend operator fun invoke(youtubeKey: String, geminiKey: String) = repo.refresh(youtubeKey, geminiKey)
}
