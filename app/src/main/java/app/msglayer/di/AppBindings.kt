package app.msglayer.di

import app.msglayer.data.source.MockMessageSource
import app.msglayer.domain.ai.AiProvider
import app.msglayer.domain.ai.LocalRetrievalAiProvider
import app.msglayer.domain.source.MessageSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds @Singleton
    abstract fun bindMessageSource(impl: MockMessageSource): MessageSource

    @Binds @Singleton
    abstract fun bindAiProvider(impl: LocalRetrievalAiProvider): AiProvider
}
