package app.msglayer.data.source

import app.msglayer.data.mock.MockSmsDataset
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.SourceCapabilities
import app.msglayer.domain.source.MessageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMessageSource @Inject constructor() : MessageSource {
    override val id: String = "mock-sms"
    override val displayName: String = "Mock SMS"
    override val capabilities: SourceCapabilities = SourceCapabilities(
        canRead = true,
        canArchive = false,
        canMove = false,
        canMute = false,
        canDelete = false
    )

    private val messages = MutableStateFlow(MockSmsDataset.messages(System.currentTimeMillis()))

    override suspend fun getMessages(): List<Message> = messages.value

    override fun observeMessages(): Flow<List<Message>> = messages.asStateFlow()

    fun refresh(now: Long = System.currentTimeMillis()) {
        messages.value = MockSmsDataset.messages(now)
    }
}

/**
 * Future SMS ContentResolver implementation. Capabilities declared explicitly;
 * organizer never performs autonomous permanent deletion.
 */
class SmsMessageSource : MessageSource {
    override val id: String = "android-sms"
    override val displayName: String = "Device SMS"
    override val capabilities: SourceCapabilities = SourceCapabilities(
        canRead = true,
        canArchive = false,
        canMove = false,
        canMute = false,
        canDelete = false
    )

    override suspend fun getMessages(): List<Message> = emptyList()

    override fun observeMessages(): Flow<List<Message>> = MutableStateFlow(emptyList())
}
