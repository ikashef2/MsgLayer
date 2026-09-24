package app.msglayer.domain.ai

import app.msglayer.data.repository.OrganizerRepository
import app.msglayer.domain.model.AskAnswer
import app.msglayer.domain.model.ExtractedEntity
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageClassification
import app.msglayer.data.pipeline.LocalClassifier

class LocalRetrievalAiProvider constructor(
    private val classifier: LocalClassifier,
    private val repository: OrganizerRepository
) : AiProvider {
    override suspend fun classify(message: Message): MessageClassification =
        classifier.classify(message, repository.sender(message.senderId))

    override suspend fun extract(message: Message): List<ExtractedEntity> = emptyList()

    override suspend fun answer(
        question: String,
        candidates: List<Message>,
        contextNotes: List<String>
    ): AskAnswer = repository.ask(question)
}
