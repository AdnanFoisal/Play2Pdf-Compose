package com.adnanfoisal.play2pdf.ui.compile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.domain.model.Playlist
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.Topic
import com.adnanfoisal.play2pdf.domain.model.TopicSource
import com.adnanfoisal.play2pdf.domain.usecase.ExtractTopicsUseCase
import com.adnanfoisal.play2pdf.domain.usecase.FetchPlaylistMetaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompileUiState(
    val playlists: List<Playlist> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val topicInput: String = "",
    val subject: String = "",
    val author: String = "",
    val selectedTheme: PdfTheme = PdfTheme.TufteScholar,
    val playlistUrlInput: String = "",
    val isExtractingTopics: Boolean = false,
    val isFetchingMeta: Boolean = false,
    val error: String? = null,
    val canCompile: Boolean = false
)

sealed interface CompileUiEvent {
    data class PlaylistUrlChanged(val value: String) : CompileUiEvent
    data class AddPlaylist(val url: String) : CompileUiEvent
    data class RemovePlaylist(val url: String) : CompileUiEvent
    data class TopicInputChanged(val value: String) : CompileUiEvent
    data class AddTopic(val text: String) : CompileUiEvent
    data class RemoveTopic(val text: String) : CompileUiEvent
    data class SubjectChanged(val value: String) : CompileUiEvent
    data class AuthorChanged(val value: String) : CompileUiEvent
    data class ThemeChanged(val value: PdfTheme) : CompileUiEvent
    data object ExtractTopics : CompileUiEvent
    data object DismissError : CompileUiEvent
}

@HiltViewModel
class CompileViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val extractTopics: ExtractTopicsUseCase,
    private val fetchMeta: FetchPlaylistMetaUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CompileUiState())
    val state: StateFlow<CompileUiState> = _state.asStateFlow()

    init {
        // Pre-fill author from saved user name + theme from saved preference.
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update {
                it.copy(
                    author = s.userName,
                    selectedTheme = s.selectedTheme
                )
            }
        }
    }

    fun onEvent(event: CompileUiEvent) {
        when (event) {
            is CompileUiEvent.PlaylistUrlChanged -> {
                _state.update { it.copy(playlistUrlInput = event.value) }
            }
            is CompileUiEvent.AddPlaylist -> addPlaylist(event.url)
            is CompileUiEvent.RemovePlaylist -> {
                _state.update {
                    it.copy(playlists = it.playlists.filterNot { p -> p.url == event.url })
                }
                recomputeCanCompile()
            }
            is CompileUiEvent.TopicInputChanged -> {
                _state.update { it.copy(topicInput = event.value) }
            }
            is CompileUiEvent.AddTopic -> addTopic(event.text)
            is CompileUiEvent.RemoveTopic -> {
                _state.update {
                    it.copy(topics = it.topics.filterNot { t -> t.text == event.text })
                }
                recomputeCanCompile()
            }
            is CompileUiEvent.SubjectChanged -> {
                _state.update { it.copy(subject = event.value) }
                recomputeCanCompile()
            }
            is CompileUiEvent.AuthorChanged -> {
                _state.update { it.copy(author = event.value) }
            }
            is CompileUiEvent.ThemeChanged -> {
                _state.update { it.copy(selectedTheme = event.value) }
                viewModelScope.launch { settings.setSelectedTheme(event.value) }
            }
            CompileUiEvent.ExtractTopics -> extractTopicsFromPlaylists()
            CompileUiEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun addPlaylist(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        if (!trimmed.contains("list=")) {
            _state.update { it.copy(error = "That doesn't look like a YouTube playlist URL.") }
            return
        }
        if (_state.value.playlists.any { it.url == trimmed }) return
        _state.update {
            it.copy(
                playlists = it.playlists + Playlist(url = trimmed),
                playlistUrlInput = ""
            )
        }
        recomputeCanCompile()
        // Fetch meta in the background so we can show the title.
        viewModelScope.launch {
            _state.update { it.copy(isFetchingMeta = true) }
            fetchMeta(trimmed).onSuccess { meta ->
                _state.update { s ->
                    s.copy(
                        playlists = s.playlists.map {
                            if (it.url == trimmed) it.copy(
                                title = meta.title,
                                channel = meta.channel,
                                videoCount = meta.videoCount,
                                thumbnailUrl = meta.thumbnailUrl
                            ) else it
                        },
                        isFetchingMeta = false
                    )
                }
            }.onFailure { _ ->
                // Meta fetch is best-effort; ignore failures so the user can still compile.
                _state.update { it.copy(isFetchingMeta = false) }
            }
        }
    }

    private fun addTopic(text: String) {
        val trimmed = text.trim().trimEnd(',', '\n')
        if (trimmed.isEmpty()) return
        // Split on commas and newlines — user can paste "topic 1, topic 2, topic 3".
        val newTopics = trimmed.split(Regex("[,\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
        val existing = _state.value.topics.map { it.text }.toSet()
        val toAdd = newTopics.filter { it !in existing }.map { Topic(it, TopicSource.Manual) }
        if (toAdd.isEmpty()) {
            _state.update { it.copy(topicInput = "") }
            return
        }
        _state.update {
            it.copy(
                topics = it.topics + toAdd,
                topicInput = ""
            )
        }
        recomputeCanCompile()
    }

    private fun extractTopicsFromPlaylists() {
        val urls = _state.value.playlists.map { it.url }
        if (urls.isEmpty()) {
            _state.update { it.copy(error = "Add a playlist first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isExtractingTopics = true, error = null) }
            extractTopics(urls).onSuccess { topics ->
                val existing = _state.value.topics.map { t -> t.text }.toSet()
                val toAdd = topics.filter { it !in existing }.map { Topic(it, TopicSource.AutoExtracted) }
                _state.update {
                    it.copy(
                        topics = it.topics + toAdd,
                        isExtractingTopics = false
                    )
                }
                recomputeCanCompile()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isExtractingTopics = false,
                        error = e.message ?: "Topic extraction failed."
                    )
                }
            }
        }
    }

    private fun recomputeCanCompile() {
        val s = _state.value
        _state.update {
            it.copy(
                canCompile = s.playlists.isNotEmpty() &&
                    s.topics.isNotEmpty() &&
                    s.subject.isNotBlank() &&
                    s.author.isNotBlank()
            )
        }
    }
}
