package com.adnanfoisal.play2pdf.domain.usecase

import com.adnanfoisal.play2pdf.data.repository.CompileRepository
import com.adnanfoisal.play2pdf.domain.model.CompileStep
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Compiles a study guide PDF.
 *
 * Wraps [CompileRepository.generateGuide] and emits a stream of
 * [CompileStep]s so the Compiling screen can show a progress checklist.
 *
 * The backend doesn't expose per-step progress (it's a single POST that
 * returns the finished PDF), so we emit steps heuristically:
 *  - "Connecting" immediately
 *  - "Fetching videos" after 200ms
 *  - "Extracting topics" after the request has been in-flight for 2s
 *  - "Matching topics" after 4s
 *  - "Rendering PDF" after 8s
 *  - "Done" when the response arrives
 *
 * This isn't a real progress bar — it's a "we're working on it"
 * reassurance pattern. The Compiling screen also shows a spinner.
 */
class CompileGuideUseCase @Inject constructor(
    private val repo: CompileRepository
) {
    operator fun invoke(
        subject: String,
        author: String,
        playlistUrls: List<String>,
        topics: List<String>,
        theme: PdfTheme
    ): Flow<CompileState> = channelFlow {
        send(CompileState.Step(CompileStep.Connecting))
        delay(200)
        send(CompileState.Step(CompileStep.FetchingVideos))

        val progressJob = launch {
            delay(2000)
            send(CompileState.Step(CompileStep.ExtractingTopics))
            delay(2000)
            send(CompileState.Step(CompileStep.MatchingTopics))
            delay(4000)
            send(CompileState.Step(CompileStep.RenderingPdf))
        }

        val result = repo.generateGuide(subject, author, playlistUrls, topics, theme)
        
        progressJob.cancel()
        send(CompileState.Result(result))
    }
}

sealed class CompileState {
    data class Step(val step: com.adnanfoisal.play2pdf.domain.model.CompileStep) : CompileState()
    data class Result(val outcome: com.adnanfoisal.play2pdf.data.repository.CompileResult) : CompileState()
}
