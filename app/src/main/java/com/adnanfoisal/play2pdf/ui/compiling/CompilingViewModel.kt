package com.adnanfoisal.play2pdf.ui.compiling

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.core.haptics.HapticsManager
import com.adnanfoisal.play2pdf.core.notification.PdfReadyNotifier
import com.adnanfoisal.play2pdf.core.sound.SoundEffect
import com.adnanfoisal.play2pdf.core.sound.SoundManager
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.data.repository.CompileResult
import com.adnanfoisal.play2pdf.data.repository.HistoryRepository
import com.adnanfoisal.play2pdf.domain.model.CompileStep
import com.adnanfoisal.play2pdf.domain.model.PdfHistory
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.SharedCompileState
import com.adnanfoisal.play2pdf.domain.usecase.CompileGuideUseCase
import com.adnanfoisal.play2pdf.domain.usecase.CompileState
import com.adnanfoisal.play2pdf.domain.usecase.SavePdfToDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CompilingUiState(
    val phase: CompilingPhase = CompilingPhase.InProgress,
    val currentStep: CompileStep = CompileStep.Connecting,
    val completedSteps: Set<CompileStep> = emptySet(),
    val pdfFile: File? = null,
    val pdfSizeBytes: Long? = null,
    val errorMessage: String? = null,
    /** Row id of the history entry created on success; null until then. */
    val historyId: Long? = null
)

sealed interface CompilingPhase {
    data object InProgress : CompilingPhase
    data object Success : CompilingPhase
    data object Error : CompilingPhase
}

@HiltViewModel
class CompilingViewModel @Inject constructor(
    private val compileGuide: CompileGuideUseCase,
    private val savePdf: SavePdfToDownloadsUseCase,
    private val settings: SettingsRepository,
    private val sharedCompileState: SharedCompileState,
    private val historyRepository: HistoryRepository,
    private val haptics: HapticsManager,
    private val sounds: SoundManager,
    private val notifier: PdfReadyNotifier,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CompilingUiState())
    val state: StateFlow<CompilingUiState> = _state.asStateFlow()

    init {
        start()
    }

    private fun start() {
        viewModelScope.launch {
            // A5: medium tick when the compile actually starts (skip the
            // cosmetic 'Connecting' step — no haptic spam).
            var realStepTicked = false
            compileGuide(
                sharedCompileState.subject,
                sharedCompileState.author,
                sharedCompileState.playlistUrls,
                sharedCompileState.topics,
                sharedCompileState.theme
            ).collect { state ->
                when (state) {
                    is CompileState.Step -> {
                        if (!realStepTicked && state.step != CompileStep.Connecting) {
                            realStepTicked = true
                            haptics.medium()
                            sounds.play(SoundEffect.Tap)
                        }
                        _state.update {
                            it.copy(
                                currentStep = state.step,
                                completedSteps = it.completedSteps + state.step
                            )
                        }
                    }
                    is CompileState.Result -> handleResult(state.outcome)
                }
            }
        }
    }

    fun retry() {
        _state.update {
            it.copy(
                phase = CompilingPhase.InProgress,
                errorMessage = null,
                currentStep = CompileStep.Connecting,
                completedSteps = emptySet(),
                historyId = null
            )
        }
        start()
    }

    fun saveToDownloads(onSaved: (android.net.Uri?) -> Unit) {
        val file = _state.value.pdfFile ?: return
        viewModelScope.launch {
            val s = settings.settings.first()
            val displayName = "${s.userName.ifBlank { "study_guide" }}_${System.currentTimeMillis()}"
            val uri = savePdf(file, displayName)

            // Re-point the existing history entry at the permanent MediaStore
            // copy so it survives cache eviction. (The row was already created
            // on success — see handleResult.)
            val id = _state.value.historyId
            if (uri != null && id != null) {
                historyRepository.getById(id)?.let { entry ->
                    historyRepository.update(entry.copy(pdfUri = uri.toString()))
                }
            }
            onSaved(uri)
        }
    }

    private suspend fun handleResult(result: CompileResult) {
        when (result) {
            is CompileResult.Success -> {
                // Persist to history the moment the compile succeeds — not only
                // when the user taps "Save to Downloads". Routed through
                // HistoryRepository so the 30-row cap actually applies.
                val shareUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    result.pdfFile
                )
                val id = historyRepository.insert(
                    PdfHistory(
                        subject = sharedCompileState.subject,
                        author = sharedCompileState.author,
                        playlistUrls = sharedCompileState.playlistUrls,
                        topics = sharedCompileState.topics,
                        // PdfHistory carries the enum; the repository persists
                        // theme.apiName (NOT theme.name) so History can decode it.
                        theme = sharedCompileState.theme,
                        createdAtEpochMs = System.currentTimeMillis(),
                        // Shareable FileProvider URI for the cached PDF; swapped
                        // for a MediaStore URI if the user saves to Downloads.
                        pdfUri = shareUri.toString(),
                        pdfSizeBytes = result.sizeBytes,
                        videoCount = sharedCompileState.videoCount,
                        topicCount = sharedCompileState.topics.size
                    )
                )
                _state.update {
                    it.copy(
                        phase = CompilingPhase.Success,
                        pdfFile = result.pdfFile,
                        pdfSizeBytes = result.sizeBytes,
                        historyId = id,
                        currentStep = CompileStep.Done,
                        completedSteps = it.completedSteps + CompileStep.Done
                    )
                }
                // A5: the success moment — double-tick haptic, chime (no-op
                // until sfx assets ship), and the "PDF ready" notification.
                haptics.success()
                sounds.play(SoundEffect.Success)
                notifier.notify(sharedCompileState.subject.ifBlank { "Study guide" })
            }
            is CompileResult.Failure -> {
                _state.update {
                    it.copy(
                        phase = CompilingPhase.Error,
                        errorMessage = result.message
                    )
                }
                haptics.error()
                sounds.play(SoundEffect.Error)
            }
        }
    }
}
