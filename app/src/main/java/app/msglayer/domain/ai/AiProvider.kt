package app.msglayer.domain.ai

import app.msglayer.domain.model.AskAnswer
import app.msglayer.domain.model.ExtractedEntity
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageClassification

/**
 * Replaceable AI backend. MVP uses local/mock retrieval — no cloud upload by default.
 */
interface AiProvider {
    suspend fun classify(message: Message): MessageClassification
    suspend fun extract(message: Message): List<ExtractedEntity>
    suspend fun answer(
        question: String,
        candidates: List<Message>,
        contextNotes: List<String> = emptyList()
    ): AskAnswer
}
