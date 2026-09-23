package app.msglayer.data.pipeline

import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.PersianText
import app.msglayer.domain.model.CurrencyUnit
import app.msglayer.domain.model.Fact
import app.msglayer.domain.model.FactType
import app.msglayer.domain.model.LabelScore
import app.msglayer.domain.model.LifecycleType
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageClassification
import app.msglayer.domain.model.MessageLabel
import app.msglayer.domain.model.MessageLifecycle
import app.msglayer.domain.model.MoneyAmount
import app.msglayer.domain.model.Sender
import app.msglayer.domain.model.Transaction
import app.msglayer.domain.model.TransactionType
import app.msglayer.domain.model.VisibilityState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalClassifier @Inject constructor() {
    fun classify(message: Message, sender: Sender?): MessageClassification {
        val body = PersianText.toEnglishDigits(message.body).lowercase()
        val scores = mutableListOf<LabelScore>()

        fun add(label: MessageLabel, conf: Float) = scores.add(LabelScore(label, conf))

        if (sender?.isKnownBank == true || body.contains("bank") || body.contains("\u0628\u0627\u0646\u06A9") || body.contains("blu") || body.contains("\u0645\u0627\u0646\u062F\u0647")) {
            add(MessageLabel.FINANCE, 0.95f)
            add(MessageLabel.BANKING, 0.97f)
        }
        if (body.contains("otp") || body.contains("code:") || message.body.contains("\u06A9\u062F") || message.body.contains("\u0631\u0645\u0632")) {
            add(MessageLabel.OTP, 0.94f)
        }
        if (sender?.isCommercial == true || body.contains("irancell") || message.body.contains("\u0627\u06CC\u0631\u0627\u0646\u0633\u0644") || body.contains("recharge") || body.contains("\u062A\u062E\u0641\u06CC\u0641")) {
            add(MessageLabel.COMMERCIAL, 0.9f)
        }
        if (body.contains("digikala") || body.contains("delivery") || message.body.contains("\u0645\u0631\u0633\u0648\u0644\u0647") || body.contains("tracking")) {
            add(MessageLabel.ORDERS, 0.92f)
            add(MessageLabel.DELIVERIES, 0.9f)
        }
        if (body.contains("bill") || message.body.contains("\u0642\u0628\u0636") || body.contains("due") || message.body.contains("\u0633\u0631\u0631\u0633\u06CC\u062F")) {
            add(MessageLabel.BILLS, 0.91f)
        }
        if (body.contains("appointment") || body.contains("reminder") || body.contains("17:30") || body.contains("dentist")) {
            add(MessageLabel.APPOINTMENTS, 0.9f)
        }
        if (body.contains("failed") || message.body.contains("\u0646\u0627\u0645\u0648\u0641\u0642") || body.contains("invoice") || body.contains("?")) {
            add(MessageLabel.IMPORTANT, 0.8f)
        }
        if (body.contains("epfund") || body.contains("report") || body.contains("invoice")) {
            add(MessageLabel.WORK, 0.88f)
        }
        if (body.contains("spam") || body.contains("win prize")) {
            add(MessageLabel.SPAM, 0.85f)
        }
        if (sender?.id in setOf("s-ima", "s-reza", "s-ali") && scores.none { it.label == MessageLabel.WORK }) {
            add(MessageLabel.PERSONAL, 0.8f)
        }
        if (scores.isEmpty()) add(MessageLabel.UNKNOWN, 0.4f)

        val primary = scores.maxByOrNull { it.confidence }?.label ?: MessageLabel.UNKNOWN
        return MessageClassification(message.id, scores.distinctBy { it.label }, primary)
    }

    fun lifecycle(message: Message, classification: MessageClassification): MessageLifecycle {
        val labels = classification.labels.map { it.label }.toSet()
        return when {
            MessageLabel.OTP in labels -> MessageLifecycle(
                LifecycleType.EXPIRING,
                expiresAt = message.timestamp + 15 * 60_000,
                reason = "OTP"
            )
            MessageLabel.COMMERCIAL in labels || MessageLabel.SPAM in labels ->
                MessageLifecycle(LifecycleType.DISPOSABLE, reason = "promotional")
            MessageLabel.FINANCE in labels || MessageLabel.DELIVERIES in labels || MessageLabel.APPOINTMENTS in labels ->
                MessageLifecycle(LifecycleType.SUPERSEDED, reason = "stateful")
            else -> MessageLifecycle(LifecycleType.PERSISTENT)
        }
    }

    fun visibility(classification: MessageClassification, lifecycle: MessageLifecycle): VisibilityState {
        val labels = classification.labels.map { it.label }.toSet()
        return when {
            MessageLabel.SPAM in labels -> VisibilityState.HIDDEN
            MessageLabel.COMMERCIAL in labels -> VisibilityState.HIDDEN
            lifecycle.type == LifecycleType.EXPIRING && lifecycle.expiresAt != null &&
                System.currentTimeMillis() > lifecycle.expiresAt -> VisibilityState.COLLAPSED_GROUP
            MessageLabel.OTP in labels -> VisibilityState.COLLAPSED_GROUP
            else -> VisibilityState.PRIMARY
        }
    }
}

@Singleton
class FinanceExtractor @Inject constructor() {
    fun extractTransactions(message: Message, accountId: String?): Transaction? {
        val body = message.body
        val money = MoneyNormalizer.parse(body) ?: return null
        if (!body.contains("\u0645\u0627\u0646\u062F\u0647", ignoreCase = true) &&
            !body.contains("balance", ignoreCase = true) &&
            !body.contains("\u0628\u0631\u062F\u0627\u0634\u062A") &&
            !body.contains("\u067E\u0631\u062F\u0627\u062E\u062A") &&
            !body.contains("snapp", ignoreCase = true) &&
            !body.contains("\u0642\u0628\u0636")
        ) {
            // still allow if unit is clear and banking sender implied via accountId
            if (accountId == null) return null
        }
        val lower = PersianText.toEnglishDigits(body).lowercase()
        val type = when {
            lower.contains("failed") || body.contains("\u0646\u0627\u0645\u0648\u0641\u0642") -> TransactionType.UNKNOWN
            lower.contains("snapp") || body.contains("\u0628\u0631\u062F\u0627\u0634\u062A") || lower.contains("expense") -> TransactionType.EXPENSE
            body.contains("\u0648\u0627\u0631\u06CC\u0632") || lower.contains("deposit") || lower.contains("income") -> TransactionType.INCOME
            lower.contains("fee") -> TransactionType.FEE
            body.contains("\u0645\u0627\u0646\u062F\u0647") || lower.contains("balance") -> TransactionType.UNKNOWN
            else -> TransactionType.EXPENSE
        }
        val balanceAfter = Regex("""(?:\u0645\u0627\u0646\u062F\u0647|balance)\s*[:\u063A]?\s*([\d\u06F0-\u06F9\u066C,\s]+)""", RegexOption.IGNORE_CASE)
            .find(PersianText.toEnglishDigits(body))
            ?.groupValues?.getOrNull(1)
            ?.let { MoneyNormalizer.parse(it + " \u062A\u0648\u0645\u0627\u0646")?.toman }

        val amount = MoneyAmount(
            amountMinor = money.rawNumber,
            unit = money.unitHint,
            amountToman = money.toman,
            originalText = money.originalText,
            conversionConfident = money.conversionConfident
        )
        return Transaction(
            id = "tx-${message.id}",
            accountId = accountId ?: "unknown",
            type = type,
            amount = amount,
            timestamp = message.timestamp,
            merchant = detectMerchant(body),
            balanceAfterToman = balanceAfter ?: money.toman.takeIf {
                body.contains("\u0645\u0627\u0646\u062F\u0647") || body.contains("balance", true)
            },
            reference = null,
            sourceMessageId = message.id,
            confidence = if (money.conversionConfident) 0.9f else 0.5f
        )
    }

    fun extractBalanceFact(message: Message, accountEntityId: String): Fact? {
        val body = PersianText.toEnglishDigits(message.body)
        val match = Regex("""(?:\u0645\u0627\u0646\u062F\u0647|balance)\s*[:\u063A]?\s*([\d\u06F0-\u06F9\u066C,\s]+)\s*(\u062A\u0648\u0645\u0627\u0646|toman|t|rial|\u0631\u06CC\u0627\u0644)?""", RegexOption.IGNORE_CASE)
            .find(body) ?: return null
        val parsed = MoneyNormalizer.parse(match.value) ?: return null
        if (!parsed.conversionConfident || parsed.toman == null) return null
        return Fact(
            id = "fact-bal-${message.id}",
            type = FactType.BANK_BALANCE,
            entityId = accountEntityId,
            value = parsed.originalText,
            normalizedValue = parsed.toman.toString(),
            displayValue = MoneyNormalizer.formatToman(parsed.toman),
            validFrom = message.timestamp,
            confidence = 0.92f,
            sourceMessageIds = listOf(message.id),
            freshnessTimestamp = message.timestamp
        )
    }

    private fun detectMerchant(body: String): String? = when {
        body.contains("Snapp", true) || body.contains("snapp", true) -> "Snapp"
        body.contains("Digikala", true) -> "Digikala"
        else -> null
    }
}
