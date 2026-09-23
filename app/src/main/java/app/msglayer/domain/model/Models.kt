package app.msglayer.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageSourceType {
    SMS,
    TELEGRAM,
    WHATSAPP,
    EMAIL,
    NOTIFICATION,
    MOCK
}

@Serializable
data class SourceCapabilities(
    val canRead: Boolean = true,
    val canArchive: Boolean = false,
    val canMove: Boolean = false,
    val canMute: Boolean = false,
    val canDelete: Boolean = false
)

@Serializable
enum class MessageLabel {
    PERSONAL,
    IMPORTANT,
    WORK,
    FINANCE,
    BANKING,
    OTP,
    ORDERS,
    DELIVERIES,
    BILLS,
    APPOINTMENTS,
    COMMERCIAL,
    SPAM,
    SYSTEM,
    UNKNOWN
}

@Serializable
enum class LifecycleType {
    PERSISTENT,
    EXPIRING,
    SUPERSEDED,
    DISPOSABLE
}

@Serializable
enum class VisibilityState {
    PRIMARY,
    HIDDEN,
    ARCHIVED,
    COLLAPSED_GROUP
}

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    WITHDRAWAL,
    DEPOSIT,
    FEE,
    UNKNOWN
}

@Serializable
enum class CurrencyUnit {
    TOMAN,
    RIAL,
    UNKNOWN
}

@Serializable
enum class FactType {
    BANK_BALANCE,
    ADDRESS,
    PHONE_NUMBER,
    BANK_CARD,
    IBAN,
    APPOINTMENT_TIME,
    DELIVERY_STATE,
    BILL_DUE_DATE,
    TRACKING_CODE,
    OTP,
    PERSON_RELATION,
    PAYMENT_AMOUNT,
    MEETING_TIME,
    OTHER
}

@Serializable
enum class SmartViewType {
    NEEDS_ATTENTION,
    FINANCE,
    OTP,
    ORDERS,
    BILLS,
    COMMERCIAL,
    HIDDEN,
    RECENTLY_UPDATED,
    IMPORTANT,
    PEOPLE
}

@Serializable
enum class AttentionKind {
    UNANSWERED_QUESTION,
    REQUESTED_FILE,
    REQUESTED_PAYMENT,
    UPCOMING_DEADLINE,
    UNPAID_BILL,
    FAILED_TRANSACTION,
    APPOINTMENT_SOON,
    EXPIRING_PICKUP
}

@Serializable
enum class RuleActionType {
    LABEL,
    GROUP,
    HIDE,
    SHOW,
    ARCHIVE,
    PRIORITIZE,
    MUTE,
    MARK_IMPORTANT
}

@Serializable
enum class OrganizationActionType {
    GROUPED,
    COLLAPSED,
    HIDDEN,
    LABELED,
    ARCHIVED,
    PRIORITIZED,
    MUTED,
    SUPERSEDED,
    RULE_APPLIED
}

@Serializable
data class Sender(
    val id: String,
    val displayName: String,
    val address: String,
    val normalizedAddress: String = address,
    val isKnownBank: Boolean = false,
    val isCommercial: Boolean = false
)

@Serializable
data class Conversation(
    val id: String,
    val senderId: String,
    val title: String,
    val lastMessageAt: Long,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val userFolder: String? = null
)

@Serializable
data class LabelScore(
    val label: MessageLabel,
    val confidence: Float
)

@Serializable
data class MessageClassification(
    val messageId: String,
    val labels: List<LabelScore>,
    val primaryLabel: MessageLabel,
    val explanation: String? = null
)

@Serializable
data class MessageLifecycle(
    val type: LifecycleType,
    val expiresAt: Long? = null,
    val supersededByMessageId: String? = null,
    val reason: String? = null
)

@Serializable
data class Message(
    val id: String,
    val sourceType: MessageSourceType,
    val conversationId: String,
    val senderId: String,
    val body: String,
    val bodyOriginal: String = body,
    val timestamp: Long,
    val isRead: Boolean = true,
    val isOutgoing: Boolean = false,
    val threadKey: String = conversationId,
    val classification: MessageClassification? = null,
    val lifecycle: MessageLifecycle = MessageLifecycle(LifecycleType.PERSISTENT),
    val visibility: VisibilityState = VisibilityState.PRIMARY,
    val priority: Int = 0,
    val groupKey: String? = null
)

@Serializable
data class ExtractedEntity(
    val id: String,
    val type: FactType,
    val rawValue: String,
    val normalizedValue: String,
    val messageId: String,
    val confidence: Float
)

@Serializable
data class Fact(
    val id: String,
    val type: FactType,
    val entityId: String? = null,
    val value: String,
    val normalizedValue: String,
    val displayValue: String = value,
    val validFrom: Long,
    val validUntil: Long? = null,
    val supersededBy: String? = null,
    val confidence: Float,
    val sourceMessageIds: List<String>,
    val freshnessTimestamp: Long = validFrom,
    val isCurrent: Boolean = true
)

@Serializable
data class BankAccount(
    val id: String,
    val bankName: String,
    val accountHint: String? = null,
    val cardHint: String? = null,
    val lastKnownBalanceToman: Long? = null,
    val balanceUpdatedAt: Long? = null,
    val balanceSourceMessageId: String? = null,
    val balanceConfidence: Float = 0f
)

@Serializable
data class MoneyAmount(
    val amountMinor: Long,
    val unit: CurrencyUnit,
    val amountToman: Long?,
    val originalText: String,
    val conversionConfident: Boolean
)

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val type: TransactionType,
    val amount: MoneyAmount,
    val timestamp: Long,
    val merchant: String? = null,
    val balanceAfterToman: Long? = null,
    val reference: String? = null,
    val sourceMessageId: String,
    val confidence: Float
)

@Serializable
data class AttentionItem(
    val id: String,
    val kind: AttentionKind,
    val title: String,
    val subtitle: String,
    val messageId: String,
    val timestamp: Long,
    val urgency: Int = 0
)

@Serializable
data class ChangeEvent(
    val id: String,
    val text: String,
    val timestamp: Long,
    val relatedMessageIds: List<String> = emptyList(),
    val category: String? = null
)

@Serializable
data class AutomationRule(
    val id: String,
    val naturalLanguage: String,
    val conditionsSummary: String,
    val actions: List<RuleActionType>,
    val enabled: Boolean = true,
    val createdAt: Long
)

@Serializable
data class OrganizationAction(
    val id: String,
    val type: OrganizationActionType,
    val description: String,
    val timestamp: Long,
    val affectedMessageIds: List<String>,
    val ruleId: String? = null,
    val undoable: Boolean = true,
    val undone: Boolean = false
)

@Serializable
data class UserCorrection(
    val id: String,
    val messageId: String? = null,
    val senderId: String? = null,
    val correctionType: String,
    val payload: String,
    val createdAt: Long
)

@Serializable
data class SearchIndexEntry(
    val id: String,
    val messageId: String?,
    val factId: String? = null,
    val transactionId: String? = null,
    val text: String,
    val tokens: List<String>,
    val labels: List<MessageLabel> = emptyList(),
    val timestamp: Long,
    val amountToman: Long? = null
)

@Serializable
data class AiEvidence(
    val messageIds: List<String>,
    val factIds: List<String> = emptyList(),
    val transactionIds: List<String> = emptyList(),
    val note: String? = null
)

@Serializable
data class AskAnswer(
    val question: String,
    val answerText: String,
    val structuredLines: List<String> = emptyList(),
    val evidence: AiEvidence,
    val freshnessNote: String? = null,
    val confidenceNote: String? = null
)

@Serializable
data class MessageGroupSummary(
    val groupKey: String,
    val title: String,
    val count: Int,
    val reason: String,
    val messageIds: List<String>,
    val visibility: VisibilityState = VisibilityState.HIDDEN
)

@Serializable
data class OverviewSnapshot(
    val greeting: String,
    val attentionCount: Int,
    val attentionItems: List<AttentionItem>,
    val accounts: List<BankAccount>,
    val spentTodayToman: Long,
    val ordersArrivingToday: Int,
    val changeEvents: List<ChangeEvent>,
    val cleanedUp: List<MessageGroupSummary>,
    val knownTotalBalanceToman: Long?
)
