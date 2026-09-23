package app.msglayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.TimeFormat
import app.msglayer.data.repository.InboxFilter
import app.msglayer.data.repository.OrganizerRepository
import app.msglayer.data.repository.OrganizerState
import app.msglayer.domain.model.AskAnswer
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.OverviewSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    val overview: StateFlow<OverviewSnapshot> = repo.state
        .map { repo.overview() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.overview())
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    private val filter = MutableStateFlow(InboxFilter.PRIMARY)
    val filterState = filter.asStateFlow()
    val messages: StateFlow<List<Message>> = repo.state
        .map { repo.messages(filter.value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.messages(InboxFilter.PRIMARY))

    fun setFilter(f: InboxFilter) {
        filter.value = f
        // trigger recomputation via state already; messages map uses filter.value at emit time
    }

    fun refreshMessages(): List<Message> = repo.messages(filter.value)
    fun senderName(id: String) = repo.sender(id)?.displayName ?: id
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    repo: OrganizerRepository
) : ViewModel() {
    val state: StateFlow<OrganizerState> = repo.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)

    fun format(amount: Long?) = amount?.let { MoneyNormalizer.formatToman(it) } ?: "—"
    fun relative(ts: Long?) = ts?.let { TimeFormat.relative(System.currentTimeMillis(), it) } ?: "—"
}

@HiltViewModel
class AskViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    private val _answer = MutableStateFlow<AskAnswer?>(null)
    val answer = _answer.asStateFlow()

    fun ask(q: String) {
        if (q.isBlank()) return
        _answer.value = repo.ask(q.trim())
    }

    fun message(id: String) = repo.message(id)
    fun senderName(id: String) = repo.sender(id)?.displayName ?: id
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    private val _results = MutableStateFlow<List<Message>>(emptyList())
    val results = _results.asStateFlow()
    fun search(q: String) { _results.value = repo.search(q) }
    fun senderName(id: String) = repo.sender(id)?.displayName ?: id
}

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    val state = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun toggle(id: String) = repo.toggleRule(id)
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    val state = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun undo(id: String) = repo.undoAction(id)
}

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val repo: OrganizerRepository
) : ViewModel() {
    fun message(id: String) = repo.message(id)
    fun sender(id: String) = repo.sender(id)
    fun relatedFacts(messageId: String) = repo.state.value.facts.filter { messageId in it.sourceMessageIds }
}
