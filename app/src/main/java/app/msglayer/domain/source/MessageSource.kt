package app.msglayer.domain.source

import app.msglayer.domain.model.Message
import app.msglayer.domain.model.SourceCapabilities
import kotlinx.coroutines.flow.Flow

interface MessageSource {
    val id: String
    val displayName: String
    val capabilities: SourceCapabilities

    suspend fun getMessages(): List<Message>
    fun observeMessages(): Flow<List<Message>>
}
