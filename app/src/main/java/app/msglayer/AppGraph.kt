
package app.msglayer

import app.msglayer.data.pipeline.FinanceExtractor
import app.msglayer.data.pipeline.LocalClassifier
import app.msglayer.data.repository.OrganizerRepository
import app.msglayer.data.source.MockMessageSource

object AppGraph {
    private val source by lazy { MockMessageSource() }
    private val classifier by lazy { LocalClassifier() }
    private val finance by lazy { FinanceExtractor() }
    val repository by lazy { OrganizerRepository(source, classifier, finance) }
}
