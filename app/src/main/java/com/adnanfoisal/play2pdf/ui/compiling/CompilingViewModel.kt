package com.adnanfoisal.play2pdf.ui.compiling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.data.repository.CompileResult
import com.adnanfoisal.play2pdf.domain.model.CompileStep
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.usecase.CompileGuideUseCase
import com.adnanfoisal.play2pdf.domain.usecase.CompileState
import com.adnanfoisal.play2pdf.domain.usecase.SavePdfToDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val errorMessage: String? = null
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
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CompilingUiState())
    val state: StateFlow<CompilingUiState> = _state.asStateFlow()

    fun start(
        subject: String,
        author: String,
        playlistUrls: List<String>,
        topics: List<String>,
        theme: PdfTheme
    ) {
        viewModelScope.launch {
            compileGuide(subject, author, playlistUrls, topics, theme).collect { state ->
                when (state) {
                    is CompileState.Step -> _state.update {
                        it.copy(
                            currentStep = state.step,
                            completedSteps = it.completedSteps + state.step
                        )
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
                completedSteps = emptySet()
            )
        }
    }

    fun saveToDownloads(onSaved: (android.net.Uri?) -> Unit) {
        val file = _state.value.pdfFile ?: return
        viewModelScope.launch {
            val s = settings.settings.first()
            val displayName = "${s.userName.ifBlank { "study_guide" }}_${System.currentTimeMillis()}"
            val uri = savePdf(file, displayName)
            onSaved(uri)
        }
    }

    private fun handleResult(result: CompileResult) {
        when (result) {
            is CompileResult.Success -> _state.update {
                it.copy(
                    phase = CompilingPhase.Success,
                    pdfFile = result.pdfFile,
                    pdfSizeBytes = result.sizeBytes,
                    currentStep = CompileStep.Done,
                    completedSteps = it.completedSteps + CompileStep.Done
                )
            }
            is CompileResult.Failure -> _state.update {
                it.copy(
                    phase = CompilingPhase.Error,
                    errorMessage = result.message
                )
            }
        }
    }
}
