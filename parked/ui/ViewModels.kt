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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverviewViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    val overview: StateFlow<OverviewSnapshot> = repo.state
        .map { repo.overview() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.overview())
}

class InboxViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
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

class FinanceViewModel(repo: OrganizerRepository = app.msglayer.AppGraph.repository) : ViewModel() {
    val state: StateFlow<OrganizerState> = repo.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)

    fun format(amount: Long?) = amount?.let { MoneyNormalizer.formatToman(it) } ?: "—"
    fun relative(ts: Long?) = ts?.let { TimeFormat.relative(System.currentTimeMillis(), it) } ?: "—"
}

class AskViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    private val _answer = MutableStateFlow<AskAnswer?>(null)
    val answer = _answer.asStateFlow()

    fun ask(q: String) {
        if (q.isBlank()) return
        _answer.value = repo.ask(q.trim())
    }

    fun message(id: String) = repo.message(id)
    fun senderName(id: String) = repo.sender(id)?.displayName ?: id
}

class SearchViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    private val _results = MutableStateFlow<List<Message>>(emptyList())
    val results = _results.asStateFlow()
    fun search(q: String) { _results.value = repo.search(q) }
    fun senderName(id: String) = repo.sender(id)?.displayName ?: id
}

class RulesViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    val state = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun toggle(id: String) = repo.toggleRule(id)
}

class ActivityViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    val state = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun undo(id: String) = repo.undoAction(id)
}

class MessageDetailViewModel(private val repo: OrganizerRepository = app.msglayer.AppGraph.repository = app.msglayer.AppGraph.repository) : ViewModel() {
    fun message(id: String) = repo.message(id)
    fun sender(id: String) = repo.sender(id)
    fun relatedFacts(messageId: String) = repo.state.value.facts.filter { messageId in it.sourceMessageIds }
}
