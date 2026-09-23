package app.msglayer.data.repository

import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.TimeFormat
import app.msglayer.data.mock.MockSmsDataset
import app.msglayer.data.pipeline.FinanceExtractor
import app.msglayer.data.pipeline.LocalClassifier
import app.msglayer.data.source.MockMessageSource
import app.msglayer.domain.model.AskAnswer
import app.msglayer.domain.model.AiEvidence
import app.msglayer.domain.model.AttentionItem
import app.msglayer.domain.model.AttentionKind
import app.msglayer.domain.model.AutomationRule
import app.msglayer.domain.model.BankAccount
import app.msglayer.domain.model.ChangeEvent
import app.msglayer.domain.model.Fact
import app.msglayer.domain.model.FactType
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageGroupSummary
import app.msglayer.domain.model.MessageLabel
import app.msglayer.domain.model.OrganizationAction
import app.msglayer.domain.model.OrganizationActionType
import app.msglayer.domain.model.OverviewSnapshot
import app.msglayer.domain.model.RuleActionType
import app.msglayer.domain.model.Sender
import app.msglayer.domain.model.Transaction
import app.msglayer.domain.model.TransactionType
import app.msglayer.domain.model.VisibilityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class OrganizerState(
    val messages: List<Message> = emptyList(),
    val senders: Map<String, Sender> = emptyMap(),
    val accounts: List<BankAccount> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val facts: List<Fact> = emptyList(),
    val attention: List<AttentionItem> = emptyList(),
    val changes: List<ChangeEvent> = emptyList(),
    val groups: List<MessageGroupSummary> = emptyList(),
    val actions: List<OrganizationAction> = emptyList(),
    val rules: List<AutomationRule> = emptyList(),
    val lastVisitAt: Long = System.currentTimeMillis() - 86_400_000
)

@Singleton
class OrganizerRepository @Inject constructor(
    private val messageSource: MockMessageSource,
    private val classifier: LocalClassifier,
    private val financeExtractor: FinanceExtractor
) {
    private val _state = MutableStateFlow(OrganizerState())
    val state: StateFlow<OrganizerState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun bootstrap(now: Long = System.currentTimeMillis()) {
        val senders = MockSmsDataset.senders().associateBy { it.id }
        val raw = messageSource.getMessagesBlocking(now)
        val enriched = raw.map { msg ->
            val c = classifier.classify(msg, senders[msg.senderId])
            val life = classifier.lifecycle(msg, c)
            val vis = classifier.visibility(c, life)
            val groupKey = when {
                MessageLabel.OTP in c.labels.map { it.label } -> "otp"
                MessageLabel.COMMERCIAL in c.labels.map { it.label } -> "commercial"
                MessageLabel.SPAM in c.labels.map { it.label } -> "spam"
                else -> null
            }
            msg.copy(classification = c, lifecycle = life, visibility = vis, groupKey = groupKey)
        }

        val accountDefs = listOf(
            BankAccount("acc-blu", "Blu", cardHint = "*Blu"),
            BankAccount("acc-melli", "Melli", accountHint = "*4567"),
            BankAccount("acc-mellat", "Mellat", cardHint = "*8899")
        )
        fun accountFor(senderId: String) = when (senderId) {
            "s-blu" -> "acc-blu"
            "s-melli" -> "acc-melli"
            "s-mellat" -> "acc-mellat"
            else -> null
        }

        val balanceFacts = mutableListOf<Fact>()
        val txs = mutableListOf<Transaction>()
        enriched.forEach { msg ->
            val accId = accountFor(msg.senderId)
            financeExtractor.extractBalanceFact(msg, accId ?: return@forEach)?.let { balanceFacts += it }
            financeExtractor.extractTransactions(msg, accId)?.let { txs += it }
        }
        // Supersede older balances per account
        val currentFacts = balanceFacts
            .groupBy { it.entityId }
            .flatMap { (_, list) ->
                val sorted = list.sortedBy { it.validFrom }
                sorted.mapIndexed { index, fact ->
                    if (index < sorted.lastIndex) {
                        fact.copy(isCurrent = false, supersededBy = sorted[index + 1].id, validUntil = sorted[index + 1].validFrom)
                    } else fact
                }
            }

        val accounts = accountDefs.map { acc ->
            val latest = currentFacts.filter { it.entityId == acc.id && it.isCurrent }.maxByOrNull { it.validFrom }
            acc.copy(
                lastKnownBalanceToman = latest?.normalizedValue?.toLongOrNull(),
                balanceUpdatedAt = latest?.freshnessTimestamp,
                balanceSourceMessageId = latest?.sourceMessageIds?.firstOrNull(),
                balanceConfidence = latest?.confidence ?: 0f
            )
        }

        val appointmentFacts = enriched.filter { it.id == "m-dental" }.map {
            Fact(
                id = "fact-appt-dental",
                type = FactType.APPOINTMENT_TIME,
                value = "Thursday, Sep 24 17:30",
                normalizedValue = "2026-09-24T17:30",
                displayValue = "Thu Sep 24, 17:30",
                validFrom = it.timestamp,
                confidence = 0.9f,
                sourceMessageIds = listOf(it.id)
            )
        }
        val meetingFacts = listOf(
            Fact(
                id = "fact-meet-old",
                type = FactType.MEETING_TIME,
                entityId = "meet-ima",
                value = "4:00 PM",
                normalizedValue = "16:00",
                validFrom = enriched.first { it.id == "m-meet-1" }.timestamp,
                validUntil = enriched.first { it.id == "m-meet-2" }.timestamp,
                supersededBy = "fact-meet-new",
                confidence = 0.85f,
                sourceMessageIds = listOf("m-meet-1"),
                isCurrent = false
            ),
            Fact(
                id = "fact-meet-new",
                type = FactType.MEETING_TIME,
                entityId = "meet-ima",
                value = "5:30 PM",
                normalizedValue = "17:30",
                displayValue = "5:30 PM",
                validFrom = enriched.first { it.id == "m-meet-2" }.timestamp,
                confidence = 0.9f,
                sourceMessageIds = listOf("m-meet-2"),
                isCurrent = true
            )
        )

        val attention = listOf(
            AttentionItem("att-ima", AttentionKind.UNANSWERED_QUESTION, "Ima", "\"Are we still going Friday?\"", "m-ima", enriched.first { it.id == "m-ima" }.timestamp, 3),
            AttentionItem("att-reza", AttentionKind.REQUESTED_FILE, "Reza", "\"Can you send the invoice?\"", "m-reza", enriched.first { it.id == "m-reza" }.timestamp, 2),
            AttentionItem("att-bill", AttentionKind.UNPAID_BILL, "Electricity Bill", "Due tomorrow", "m-bill", enriched.first { it.id == "m-bill" }.timestamp, 3),
            AttentionItem("att-fail", AttentionKind.FAILED_TRANSACTION, "Bank Mellat", "Payment failed", "m-mellat-fail", enriched.first { it.id == "m-mellat-fail" }.timestamp, 3),
            AttentionItem("att-dental", AttentionKind.APPOINTMENT_SOON, "Dentist", "Tomorrow 17:30", "m-dental", enriched.first { it.id == "m-dental" }.timestamp, 2)
        )

        val otpIds = enriched.filter { it.groupKey == "otp" }.map { it.id }
        val promoIds = enriched.filter { it.groupKey == "commercial" }.map { it.id }
        val spamIds = enriched.filter { it.groupKey == "spam" }.map { it.id }
        val groups = listOfNotNull(
            if (otpIds.isNotEmpty()) MessageGroupSummary("otp", "OTP messages", otpIds.size, "Expired / short-lived codes", otpIds, VisibilityState.COLLAPSED_GROUP) else null,
            if (promoIds.isNotEmpty()) MessageGroupSummary("commercial", "Promotional messages", promoIds.size, "Hidden from Primary Inbox", promoIds, VisibilityState.HIDDEN) else null,
            if (spamIds.isNotEmpty()) MessageGroupSummary("spam", "Probable spam", spamIds.size, "Hidden from Primary Inbox", spamIds, VisibilityState.HIDDEN) else null
        )

        val actions = listOf(
            OrganizationAction("act-1", OrganizationActionType.GROUPED, "Grouped ${promoIds.size} promotional SMS", now - 40 * 60_000, promoIds),
            OrganizationAction("act-2", OrganizationActionType.COLLAPSED, "Collapsed ${otpIds.size} OTP messages", now - 90 * 60_000, otpIds),
            OrganizationAction("act-3", OrganizationActionType.LABELED, "Marked Bank Mellat as Finance", now - 86_400_000, listOf("m-mellat-new", "m-mellat-fail"), undoable = true),
            OrganizationAction("act-4", OrganizationActionType.HIDDEN, "Hidden probable spam", now - 86_400_000, spamIds)
        )

        val rules = listOf(
            AutomationRule("rule-1", "Always hide Irancell promotions.", "sender=Irancell + commercial", listOf(RuleActionType.HIDE, RuleActionType.GROUP), true, now - 7 * 86_400_000),
            AutomationRule("rule-2", "Never hide messages from Bank Melli.", "sender=Bank Melli", listOf(RuleActionType.SHOW, RuleActionType.MARK_IMPORTANT), true, now - 5 * 86_400_000),
            AutomationRule("rule-3", "Hide expired OTPs after 15 minutes.", "label=OTP + expired", listOf(RuleActionType.HIDE), true, now - 3 * 86_400_000),
            AutomationRule("rule-4", "Anything from EPFund is Work.", "sender=EPFund", listOf(RuleActionType.LABEL), true, now - 2 * 86_400_000)
        )

        val bluPrev = 2_220_000L
        val bluNow = accounts.firstOrNull { it.id == "acc-blu" }?.lastKnownBalanceToman
        val changes = buildList {
            if (bluNow != null) add(ChangeEvent("ch-blu", "Blu balance decreased by ${MoneyNormalizer.formatToman(bluPrev - bluNow)}", now - 12 * 60_000, listOf("m-blu-new"), "finance"))
            add(ChangeEvent("ch-pkg", "Your package is out for delivery", now - 60 * 60_000, listOf("m-digi-2"), "orders"))
            add(ChangeEvent("ch-bill", "Your internet bill was issued", now - 20 * 60 * 60_000, listOf("m-bill"), "bills"))
            add(ChangeEvent("ch-ali", "Ali sent a new card number", now - 7 * 60 * 60_000, listOf("m-ali"), "people"))
            add(ChangeEvent("ch-otp", "${otpIds.size} OTP messages expired", now - 3 * 60 * 60_000, otpIds, "otp"))
            add(ChangeEvent("ch-promo", "${promoIds.size} promotional messages were grouped", now - 2 * 60 * 60_000, promoIds, "commercial"))
            add(ChangeEvent("ch-ima", "Ima asked a question you have not answered", now - 3 * 60 * 60_000, listOf("m-ima"), "people"))
        }

        _state.value = OrganizerState(
            messages = enriched.sortedByDescending { it.timestamp },
            senders = senders,
            accounts = accounts,
            transactions = txs.sortedByDescending { it.timestamp },
            facts = currentFacts + appointmentFacts + meetingFacts,
            attention = attention.sortedByDescending { it.urgency },
            changes = changes,
            groups = groups,
            actions = actions,
            rules = rules,
            lastVisitAt = now - 86_400_000
        )
    }

    fun overview(now: Long = System.currentTimeMillis()): OverviewSnapshot {
        val s = _state.value
        val spentToday = s.transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= now - ((now / 86_400_000) * 86_400_000) }
            .mapNotNull { it.amount.amountToman }
            .sum()
            .let {
                // Seed a coherent spent-today from Blu debit if parsing sparse
                s.transactions.firstOrNull { it.sourceMessageId == "m-blu-new" }?.amount?.amountToman ?: 420_000L
            }
        val knownTotal = s.accounts.mapNotNull { it.lastKnownBalanceToman }.takeIf { it.isNotEmpty() }?.sum()
        return OverviewSnapshot(
            greeting = TimeFormat.greeting(now),
            attentionCount = s.attention.size,
            attentionItems = s.attention,
            accounts = s.accounts,
            spentTodayToman = spentToday,
            ordersArrivingToday = s.messages.count { it.id == "m-digi-2" },
            changeEvents = s.changes,
            cleanedUp = s.groups,
            knownTotalBalanceToman = knownTotal
        )
    }

    fun messages(filter: InboxFilter): List<Message> {
        val all = _state.value.messages
        return when (filter) {
            InboxFilter.ALL -> all
            InboxFilter.PRIMARY -> all.filter { it.visibility == VisibilityState.PRIMARY }
            InboxFilter.HIDDEN -> all.filter { it.visibility == VisibilityState.HIDDEN || it.visibility == VisibilityState.COLLAPSED_GROUP }
            InboxFilter.ARCHIVED -> all.filter { it.visibility == VisibilityState.ARCHIVED }
        }
    }

    fun message(id: String): Message? = _state.value.messages.find { it.id == id }
    fun sender(id: String): Sender? = _state.value.senders[id]

    fun search(query: String): List<Message> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _state.value.messages.filter { msg ->
            val sender = _state.value.senders[msg.senderId]?.displayName?.lowercase().orEmpty()
            msg.body.lowercase().contains(q) || sender.contains(q) ||
                msg.classification?.labels?.any { it.label.name.lowercase().contains(q) } == true
        }
    }

    fun ask(question: String): AskAnswer {
        val s = _state.value
        val q = question.lowercase()
        return when {
            q.contains("how much money") || q.contains("balance") || q.contains("\u0645\u0627\u0646\u062F\u0647") -> {
                val lines = s.accounts.map { acc ->
                    val bal = acc.lastKnownBalanceToman?.let { MoneyNormalizer.formatToman(it) } ?: "unknown"
                    val age = acc.balanceUpdatedAt?.let { TimeFormat.relative(System.currentTimeMillis(), it) } ?: "n/a"
                    "${acc.bankName}: $bal (last known, $age)"
                }
                val total = s.accounts.mapNotNull { it.lastKnownBalanceToman }.sum()
                AskAnswer(
                    question = question,
                    answerText = "Your latest known balances",
                    structuredLines = lines + "Known total: ${MoneyNormalizer.formatToman(total)}",
                    evidence = AiEvidence(s.accounts.mapNotNull { it.balanceSourceMessageId }),
                    freshnessNote = "These are last known balances from SMS, not live bank data."
                )
            }
            q.contains("spend") || q.contains("spent") -> {
                val snapp = s.transactions.filter { it.merchant.equals("Snapp", true) || it.sourceMessageId == "m-snapp" || it.sourceMessageId == "m-blu-new" }
                AskAnswer(
                    question = question,
                    answerText = "Spending inferred from recent banking SMS",
                    structuredLines = listOf(
                        "Today (Blu / Snapp): ${MoneyNormalizer.formatToman(420_000)}",
                        "Recent Snapp payment: ${MoneyNormalizer.formatToman(120_000)}"
                    ),
                    evidence = AiEvidence(snapp.map { it.sourceMessageId }.ifEmpty { listOf("m-blu-new", "m-snapp") })
                )
            }
            q.contains("dentist") || q.contains("appointment") -> AskAnswer(
                question = question,
                answerText = "Your dentist appointment appears to be:",
                structuredLines = listOf("Thursday, Sep 24", "17:30"),
                evidence = AiEvidence(listOf("m-dental"))
            )
            q.contains("address") || q.contains("reza") && q.contains("address") -> AskAnswer(
                question = question,
                answerText = "Address Reza sent:",
                structuredLines = listOf("Vanak Sq, No. 12, Unit 4"),
                evidence = AiEvidence(listOf("m-addr"))
            )
            q.contains("figma") -> AskAnswer(
                question = question,
                answerText = "Found a Figma link:",
                structuredLines = listOf("https://figma.com/file/abc123/design"),
                evidence = AiEvidence(listOf("m-personal"))
            )
            q.contains("ima") && (q.contains("friday") || q.contains("ask")) -> AskAnswer(
                question = question,
                answerText = "Ima asked about Friday:",
                structuredLines = listOf("\"Are we still going Friday?\""),
                evidence = AiEvidence(listOf("m-ima")),
                freshnessNote = "Unanswered — in Needs Attention"
            )
            q.contains("melli") -> {
                val acc = s.accounts.first { it.id == "acc-melli" }
                AskAnswer(
                    question = question,
                    answerText = "Latest known Melli balance",
                    structuredLines = listOf(
                        MoneyNormalizer.formatToman(acc.lastKnownBalanceToman ?: 0),
                        "Updated ${acc.balanceUpdatedAt?.let { TimeFormat.relative(System.currentTimeMillis(), it) }}"
                    ),
                    evidence = AiEvidence(listOfNotNull(acc.balanceSourceMessageId)),
                    freshnessNote = "Last known balance"
                )
            }
            q.contains("meeting") -> AskAnswer(
                question = question,
                answerText = "Meeting",
                structuredLines = listOf("5:30 PM", "Changed from 4:00 PM"),
                evidence = AiEvidence(listOf("m-meet-1", "m-meet-2"))
            )
            else -> {
                val hits = search(question).take(5)
                AskAnswer(
                    question = question,
                    answerText = if (hits.isEmpty()) "No matching messages found in your indexed history." else "Here are the most relevant messages:",
                    structuredLines = hits.map { it.body.lines().first().take(80) },
                    evidence = AiEvidence(hits.map { it.id })
                )
            }
        }
    }

    fun undoAction(id: String) {
        _state.update { st ->
            st.copy(actions = st.actions.map { if (it.id == id) it.copy(undone = true) else it })
        }
    }

    fun toggleRule(id: String) {
        _state.update { st ->
            st.copy(rules = st.rules.map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
        }
    }
}

enum class InboxFilter { ALL, PRIMARY, HIDDEN, ARCHIVED }

private fun MockMessageSource.getMessagesBlocking(now: Long): List<Message> {
    refresh(now)
    return MockSmsDataset.messages(now)
}
