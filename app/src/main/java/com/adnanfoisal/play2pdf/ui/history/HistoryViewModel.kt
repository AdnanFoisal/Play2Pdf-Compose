package com.adnanfoisal.play2pdf.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adnanfoisal.play2pdf.data.repository.HistoryRepository
import com.adnanfoisal.play2pdf.domain.model.PdfHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class HistoryFilter(val label: String) {
    All("All"),
    Week("This Week"),
    Month("This Month"),
    Subject("By Subject")
}

data class HistoryUiState(
    val items: List<PdfHistory> = emptyList(),
    val query: String = "",
    val filter: HistoryFilter = HistoryFilter.All,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(HistoryFilter.All)

    val state: StateFlow<HistoryUiState> = combine(
        _query,
        _filter,
        // When the query is non-empty, search; otherwise observe all.
        _query.flatMapLatest { q ->
            if (q.isBlank()) repo.observeAll() else repo.search(q)
        }
    ) { query, filter, items ->
        val filtered = when (filter) {
            HistoryFilter.All -> items
            HistoryFilter.Week -> items.filter {
                System.currentTimeMillis() - it.createdAtEpochMs <= TimeUnit.DAYS.toMillis(7)
            }
            HistoryFilter.Month -> items.filter {
                System.currentTimeMillis() - it.createdAtEpochMs <= TimeUnit.DAYS.toMillis(30)
            }
            HistoryFilter.Subject -> items.sortedBy { it.subject }
        }
        HistoryUiState(
            items = filtered,
            query = query,
            filter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(f: HistoryFilter) { _filter.value = f }

    fun delete(item: PdfHistory) {
        viewModelScope.launch { repo.delete(item) }
    }

    fun rename(item: PdfHistory, newSubject: String) {
        viewModelScope.launch {
            repo.update(item.copy(subject = newSubject))
        }
    }
}
