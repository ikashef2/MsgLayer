package app.msglayer

import android.app.Application
import app.msglayer.data.mock.MockSmsDataset
import app.msglayer.data.pipeline.FinanceExtractor
import app.msglayer.data.pipeline.LocalClassifier
import app.msglayer.data.repository.OrganizerRepository
import app.msglayer.data.source.MockMessageSource
import app.msglayer.data.source.SmsMessageSource
import app.msglayer.domain.source.MessageSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SourceMode {
    MOCK,
    DEVICE_SMS
}

data class SourceStatus(
    val mode: SourceMode = SourceMode.MOCK,
    val displayName: String = "Mock SMS",
    val messageCount: Int = 0,
    val hasSmsPermission: Boolean = false,
    val lastSyncAt: Long? = null,
    val lastError: String? = null
)

object AppGraph {
    private lateinit var app: Application

    private val classifier by lazy { LocalClassifier() }
    private val finance by lazy { FinanceExtractor() }
    private val mockSource by lazy { MockMessageSource() }
    private val smsSource by lazy { SmsMessageSource(app) }

    val repository by lazy { OrganizerRepository(classifier, finance) }

    private val _status = MutableStateFlow(SourceStatus())
    val status: StateFlow<SourceStatus> = _status.asStateFlow()

    fun init(application: Application) {
        app = application
        refreshStatus()
        // Boot with mock so UI has content before permission / first sync.
        mockSource.refresh()
        repository.ingest(
            messages = MockSmsDataset.messages(System.currentTimeMillis()),
            senders = MockSmsDataset.senders(),
            sourceMode = SourceMode.MOCK
        )
        _status.value = _status.value.copy(
            messageCount = repository.state.value.messages.size,
            lastSyncAt = System.currentTimeMillis()
        )
    }

    fun activeSource(): MessageSource = when (_status.value.mode) {
        SourceMode.MOCK -> mockSource
        SourceMode.DEVICE_SMS -> smsSource
    }

    fun hasSmsPermission(): Boolean = smsSource.hasPermission()

    fun refreshStatus() {
        _status.value = _status.value.copy(
            hasSmsPermission = hasSmsPermission(),
            displayName = activeSource().displayName
        )
    }

    suspend fun setModeAndSync(mode: SourceMode): Result<Int> {
        _status.value = _status.value.copy(mode = mode, lastError = null, displayName = when (mode) {
            SourceMode.MOCK -> mockSource.displayName
            SourceMode.DEVICE_SMS -> smsSource.displayName
        })
        return sync()
    }

    suspend fun sync(): Result<Int> {
        refreshStatus()
        return try {
            val mode = _status.value.mode
            val messages = when (mode) {
                SourceMode.MOCK -> {
                    mockSource.refresh()
                    mockSource.getMessages()
                }
                SourceMode.DEVICE_SMS -> {
                    if (!smsSource.hasPermission()) {
                        return Result.failure(IllegalStateException("SMS permission not granted"))
                    }
                    smsSource.getMessages()
                }
            }
            val senders = when (mode) {
                SourceMode.MOCK -> MockSmsDataset.senders()
                SourceMode.DEVICE_SMS -> emptyList()
            }
            repository.ingest(messages, senders, mode)
            _status.value = _status.value.copy(
                messageCount = messages.size,
                lastSyncAt = System.currentTimeMillis(),
                lastError = null,
                hasSmsPermission = hasSmsPermission()
            )
            Result.success(messages.size)
        } catch (t: Throwable) {
            _status.value = _status.value.copy(lastError = t.message ?: t.toString())
            Result.failure(t)
        }
    }
}
