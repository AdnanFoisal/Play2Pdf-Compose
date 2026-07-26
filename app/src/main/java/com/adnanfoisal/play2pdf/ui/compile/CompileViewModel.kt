package com.adnanfoisal.play2pdf.ui.compile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.adnanfoisal.play2pdf.domain.model.Playlist
import com.adnanfoisal.play2pdf.domain.model.PdfTheme
import com.adnanfoisal.play2pdf.domain.model.Topic
import com.adnanfoisal.play2pdf.domain.model.TopicSource
import com.adnanfoisal.play2pdf.domain.model.SharedCompileState
import com.adnanfoisal.play2pdf.data.db.dao.HistoryDao
import com.adnanfoisal.play2pdf.domain.usecase.ExtractTopicsUseCase
import com.adnanfoisal.play2pdf.domain.usecase.FetchPlaylistMetaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Home-screen stats shown in the GreetingHeader's StatsCard.
 *
 * Per IMPLEMENTATION_PLAN.md Step 4: `totalCount` comes from `HistoryDao.count()`
 * and `sparkline` comes from grouping `HistoryDao.observeAll()` by day-of-month.
 * Until the HistoryDao wiring lands (a follow-up), we fall back to the
 * mockup's demo curve so the UI matches `Home screen.html` visually.
 */
data class DailyCount(
    val dateLabel: String,  // e.g. "Jul 12"
    val count: Int
)

data class HomeStats(
    val totalCount: Int = 0,
    val sparkline: List<Float> = emptyList(),
    val dailyCounts: List<DailyCount> = emptyList()
)

data class CompileUiState(
    val userName: String = "",
    val stats: HomeStats = HomeStats(),
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
    private val fetchMeta: FetchPlaylistMetaUseCase,
    private val sharedCompileState: SharedCompileState,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(CompileUiState())
    val state = _state.asStateFlow()

    fun prepareForCompilation() {
        val s = _state.value
        sharedCompileState.subject = s.subject
        sharedCompileState.author = s.author
        sharedCompileState.playlistUrls = s.playlists.map { it.url }
        sharedCompileState.topics = s.topics.map { it.text }
        sharedCompileState.theme = s.selectedTheme
    }

    init {
        viewModelScope.launch {
            val s = settings.settings.first()
            _state.update {
                it.copy(
                    userName = s.userName,
                    author = s.userName,
                    selectedTheme = s.selectedTheme
                )
            }
        }
        
        viewModelScope.launch {
            historyDao.observeAll().collect { history ->
                // Build per-day counts for the last 15 days
                val cal = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val dayMs = 86_400_000L

                // Build list of last 15 days (oldest first)
                val dailyCounts = mutableListOf<DailyCount>()
                for (i in 14 downTo 0) {
                    val dayCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = dayStart + dayMs
                    val count = history.count { it.createdAtEpochMs in dayStart until dayEnd }
                    dailyCounts.add(DailyCount(
                        dateLabel = dateFormat.format(dayCal.time),
                        count = count
                    ))
                }

                // Sparkline is the float representation of daily counts
                val sparkline = if (dailyCounts.all { it.count == 0 }) {
                    // All zeros — show a flat line at 0
                    dailyCounts.map { 0f }
                } else {
                    dailyCounts.map { it.count.toFloat() }
                }

                _state.update {
                    it.copy(
                        stats = HomeStats(
                            totalCount = history.size,
                            sparkline = sparkline,
                            dailyCounts = dailyCounts
                        )
                    )
                }
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
