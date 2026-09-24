package app.msglayer.data.repository

import app.msglayer.SourceMode
import app.msglayer.core.common.MoneyNormalizer
import app.msglayer.core.common.TimeFormat
import app.msglayer.data.pipeline.BankDirectory
import app.msglayer.data.pipeline.FinanceExtractor
import app.msglayer.data.pipeline.LocalClassifier
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
    val sourceMode: SourceMode = SourceMode.MOCK,
    val lastVisitAt: Long = System.currentTimeMillis() - 86_400_000
)

class OrganizerRepository(
    private val classifier: LocalClassifier,
    private val financeExtractor: FinanceExtractor
) {
    private val _state = MutableStateFlow(OrganizerState())
    val state: StateFlow<OrganizerState> = _state.asStateFlow()

    fun ingest(
        messages: List<Message>,
        sendersSeed: List<Sender> = emptyList(),
        sourceMode: SourceMode = SourceMode.MOCK,
        now: Long = System.currentTimeMillis()
    ) {
        val senders = linkedMapOf<String, Sender>()
        sendersSeed.forEach { senders[it.id] = it }
        messages.forEach { msg ->
            if (msg.senderId !in senders) {
                val address = msg.senderId.removePrefix("sms:")
                senders[msg.senderId] = BankDirectory.senderFromAddress(address, msg.body)
            } else {
                val existing = senders.getValue(msg.senderId)
                if (!existing.isKnownBank && BankDirectory.isLikelyBank(existing.address, msg.body)) {
                    senders[msg.senderId] = existing.copy(isKnownBank = true)
                }
            }
        }

        val enriched = messages.map { msg ->
            val sender = senders[msg.senderId]
            val c = classifier.classify(msg, sender)
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

        val accountMap = linkedMapOf<String, BankAccount>()
        val balanceFacts = mutableListOf<Fact>()
        val txs = mutableListOf<Transaction>()

        enriched.forEach { msg ->
            val address = senders[msg.senderId]?.address ?: msg.senderId.removePrefix("sms:")
            val bank = BankDirectory.accountFor(address, msg.body)
            val accId = when {
                bank != null -> {
                    accountMap.putIfAbsent(
                        bank.accountId,
                        BankAccount(bank.accountId, bank.bankName, accountHint = address.takeLast(6))
                    )
                    bank.accountId
                }
                BankDirectory.isLikelyBank(address, msg.body) || senders[msg.senderId]?.isKnownBank == true -> {
                    val fallbackId = "acc-${address.hashCode().toUInt()}"
                    val name = senders[msg.senderId]?.displayName ?: address
                    accountMap.putIfAbsent(fallbackId, BankAccount(fallbackId, name, accountHint = address.takeLast(6)))
                    fallbackId
                }
                else -> null
            }
            if (accId != null) {
                financeExtractor.extractBalanceFact(msg, accId)?.let { balanceFacts += it }
                financeExtractor.extractTransactions(msg, accId)?.let { txs += it }
            }
        }

        val currentFacts = balanceFacts
            .groupBy { it.entityId }
            .flatMap { (_, list) ->
                val sorted = list.sortedBy { it.validFrom }
                sorted.mapIndexed { index, fact ->
                    if (index < sorted.lastIndex) {
                        fact.copy(
                            isCurrent = false,
                            supersededBy = sorted[index + 1].id,
                            validUntil = sorted[index + 1].validFrom
                        )
                    } else fact
                }
            }

        val accounts = accountMap.values.map { acc ->
            val latest = currentFacts.filter { it.entityId == acc.id && it.isCurrent }.maxByOrNull { it.validFrom }
            acc.copy(
                lastKnownBalanceToman = latest?.normalizedValue?.toLongOrNull(),
                balanceUpdatedAt = latest?.freshnessTimestamp,
                balanceSourceMessageId = latest?.sourceMessageIds?.firstOrNull(),
                balanceConfidence = latest?.confidence ?: 0f
            )
        }.sortedByDescending { it.balanceUpdatedAt ?: 0L }

        val appointmentFacts = enriched
            .filter { msg ->
                msg.classification?.labels?.any { it.label == MessageLabel.APPOINTMENTS } == true
            }
            .take(5)
            .map { msg ->
                Fact(
                    id = "fact-appt-${msg.id}",
                    type = FactType.APPOINTMENT_TIME,
                    value = msg.body.lines().first().take(80),
                    displayValue = msg.body.lines().first().take(80),
                    validFrom = msg.timestamp,
                    confidence = 0.7f,
                    sourceMessageIds = listOf(msg.id),
                    freshnessTimestamp = msg.timestamp
                )
            }

        val attention = buildAttention(enriched, senders)
        val otpIds = enriched.filter { it.groupKey == "otp" }.map { it.id }
        val promoIds = enriched.filter { it.groupKey == "commercial" }.map { it.id }
        val spamIds = enriched.filter { it.groupKey == "spam" }.map { it.id }
        val groups = listOfNotNull(
            if (otpIds.isNotEmpty()) MessageGroupSummary("otp", "OTP messages", otpIds.size, "Expired / short-lived codes", otpIds, VisibilityState.COLLAPSED_GROUP) else null,
            if (promoIds.isNotEmpty()) MessageGroupSummary("commercial", "Promotional messages", promoIds.size, "Hidden from Primary Inbox", promoIds, VisibilityState.HIDDEN) else null,
            if (spamIds.isNotEmpty()) MessageGroupSummary("spam", "Probable spam", spamIds.size, "Hidden from Primary Inbox", spamIds, VisibilityState.HIDDEN) else null
        )

        val actions = buildList {
            if (promoIds.isNotEmpty()) add(OrganizationAction("act-promo", OrganizationActionType.GROUPED, "Grouped ${promoIds.size} promotional SMS", now, promoIds))
            if (otpIds.isNotEmpty()) add(OrganizationAction("act-otp", OrganizationActionType.COLLAPSED, "Collapsed ${otpIds.size} OTP messages", now, otpIds))
            if (spamIds.isNotEmpty()) add(OrganizationAction("act-spam", OrganizationActionType.HIDDEN, "Hidden probable spam", now, spamIds))
            if (accounts.isNotEmpty()) add(OrganizationAction("act-fin", OrganizationActionType.LABELED, "Extracted finance from ${accounts.size} account(s)", now, accounts.mapNotNull { it.balanceSourceMessageId }, undoable = false))
        }

        val rules = listOf(
            AutomationRule("rule-1", "Hide commercial / promo SMS from Primary.", "label=COMMERCIAL", listOf(RuleActionType.HIDE, RuleActionType.GROUP), true, now),
            AutomationRule("rule-2", "Never auto-hide known bank senders.", "isKnownBank", listOf(RuleActionType.SHOW, RuleActionType.MARK_IMPORTANT), true, now),
            AutomationRule("rule-3", "Collapse expired OTPs after 15 minutes.", "label=OTP + expired", listOf(RuleActionType.HIDE), true, now)
        )

        val changes = buildChanges(enriched, accounts, txs, otpIds, promoIds, now)

        _state.value = OrganizerState(
            messages = enriched.sortedByDescending { it.timestamp },
            senders = senders,
            accounts = accounts,
            transactions = txs.sortedByDescending { it.timestamp },
            facts = currentFacts + appointmentFacts,
            attention = attention,
            changes = changes,
            groups = groups,
            actions = actions,
            rules = rules,
            sourceMode = sourceMode,
            lastVisitAt = now - 86_400_000
        )
    }

    private fun buildAttention(enriched: List<Message>, senders: Map<String, Sender>): List<AttentionItem> {
        val out = mutableListOf<AttentionItem>()
        enriched.take(200).forEach { msg ->
            val labels = msg.classification?.labels?.map { it.label }.orEmpty().toSet()
            val senderName = senders[msg.senderId]?.displayName ?: msg.senderId
            val preview = msg.body.lines().firstOrNull()?.take(72).orEmpty()
            when {
                MessageLabel.BILLS in labels -> out += AttentionItem(
                    "att-bill-${msg.id}", AttentionKind.UNPAID_BILL, senderName, preview, msg.id, msg.timestamp, 3
                )
                msg.body.contains("ناموفق") || msg.body.contains("failed", true) -> out += AttentionItem(
                    "att-fail-${msg.id}", AttentionKind.FAILED_TRANSACTION, senderName, preview, msg.id, msg.timestamp, 3
                )
                MessageLabel.APPOINTMENTS in labels -> out += AttentionItem(
                    "att-appt-${msg.id}", AttentionKind.APPOINTMENT_SOON, senderName, preview, msg.id, msg.timestamp, 2
                )
                !msg.isOutgoing && preview.contains("?") && MessageLabel.COMMERCIAL !in labels && MessageLabel.OTP !in labels ->
                    out += AttentionItem(
                        "att-q-${msg.id}", AttentionKind.UNANSWERED_QUESTION, senderName, preview, msg.id, msg.timestamp, 2
                    )
                preview.contains("invoice", true) || preview.contains("فایل") || preview.contains("بفرست") ->
                    out += AttentionItem(
                        "att-file-${msg.id}", AttentionKind.REQUESTED_FILE, senderName, preview, msg.id, msg.timestamp, 2
                    )
            }
        }
        return out.distinctBy { it.messageId }.sortedByDescending { it.urgency }.take(12)
    }

    private fun buildChanges(
        enriched: List<Message>,
        accounts: List<BankAccount>,
        txs: List<Transaction>,
        otpIds: List<String>,
        promoIds: List<String>,
        now: Long
    ): List<ChangeEvent> {
        val recent = enriched.filter { it.timestamp >= now - 48 * 3_600_000L }
        return buildList {
            accounts.filter { it.balanceUpdatedAt != null && it.balanceUpdatedAt!! >= now - 48 * 3_600_000L }
                .forEach { acc ->
                    add(
                        ChangeEvent(
                            "ch-bal-${acc.id}",
                            "${acc.bankName} balance updated to ${acc.lastKnownBalanceToman?.let { MoneyNormalizer.formatToman(it) } ?: "—"}",
                            acc.balanceUpdatedAt ?: now,
                            listOfNotNull(acc.balanceSourceMessageId),
                            "finance"
                        )
                    )
                }
            txs.take(5).forEach { tx ->
                val amt = tx.amount.amountToman?.let { MoneyNormalizer.formatToman(it) } ?: tx.amount.originalText
                add(ChangeEvent("ch-tx-${tx.id}", "${tx.type.name.lowercase()} $amt${tx.merchant?.let { " · $it" } ?: ""}", tx.timestamp, listOf(tx.sourceMessageId), "finance"))
            }
            recent.filter { it.classification?.primaryLabel == MessageLabel.DELIVERIES || it.classification?.primaryLabel == MessageLabel.ORDERS }
                .take(3)
                .forEach { msg ->
                    add(ChangeEvent("ch-ord-${msg.id}", msg.body.lines().first().take(64), msg.timestamp, listOf(msg.id), "orders"))
                }
            if (otpIds.isNotEmpty()) add(ChangeEvent("ch-otp", "${otpIds.size} OTP messages collapsed", now, otpIds.take(5), "otp"))
            if (promoIds.isNotEmpty()) add(ChangeEvent("ch-promo", "${promoIds.size} promotional messages grouped", now, promoIds.take(5), "commercial"))
        }.sortedByDescending { it.timestamp }.take(12)
    }

    fun overview(now: Long = System.currentTimeMillis()): OverviewSnapshot {
        val s = _state.value
        val dayStart = now - (now % 86_400_000L)
        val spentToday = s.transactions
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= dayStart }
            .mapNotNull { it.amount.amountToman }
            .sum()
        val knownTotal = s.accounts.mapNotNull { it.lastKnownBalanceToman }.takeIf { it.isNotEmpty() }?.sum()
        val ordersToday = s.messages.count { msg ->
            msg.timestamp >= dayStart &&
                (msg.classification?.labels?.any { it.label == MessageLabel.DELIVERIES || it.label == MessageLabel.ORDERS } == true)
        }
        return OverviewSnapshot(
            greeting = TimeFormat.greeting(now),
            attentionCount = s.attention.size,
            attentionItems = s.attention,
            accounts = s.accounts,
            spentTodayToman = spentToday,
            ordersArrivingToday = ordersToday,
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
            q.contains("how much money") || q.contains("balance") || q.contains("مانده") || q.contains("پول") -> {
                if (s.accounts.isEmpty()) {
                    AskAnswer(
                        question = question,
                        answerText = "No bank balances extracted yet.",
                        structuredLines = listOf("Sync Device SMS in Settings, or stay on Mock SMS for demo data."),
                        evidence = AiEvidence(emptyList()),
                        freshnessNote = "Balances only appear when banking SMS is present and parseable."
                    )
                } else {
                    val lines = s.accounts.map { acc ->
                        val bal = acc.lastKnownBalanceToman?.let { MoneyNormalizer.formatToman(it) } ?: "unknown"
                        val age = acc.balanceUpdatedAt?.let { TimeFormat.relative(System.currentTimeMillis(), it) } ?: "n/a"
                        "${acc.bankName}: $bal (last known, $age)"
                    }
                    val total = s.accounts.mapNotNull { it.lastKnownBalanceToman }.sum()
                    AskAnswer(
                        question = question,
                        answerText = "Your latest known balances from SMS",
                        structuredLines = lines + "Known total: ${MoneyNormalizer.formatToman(total)}",
                        evidence = AiEvidence(s.accounts.mapNotNull { it.balanceSourceMessageId }),
                        freshnessNote = "Last known from messages — not live bank data."
                    )
                }
            }
            q.contains("spend") || q.contains("spent") || q.contains("خرج") -> {
                val expenses = s.transactions.filter { it.type == TransactionType.EXPENSE }.take(8)
                if (expenses.isEmpty()) {
                    AskAnswer(
                        question = question,
                        answerText = "No expense transactions extracted yet.",
                        structuredLines = listOf("Bank debit SMS will appear here after sync."),
                        evidence = AiEvidence(emptyList())
                    )
                } else {
                    AskAnswer(
                        question = question,
                        answerText = "Recent spending from banking SMS",
                        structuredLines = expenses.map { tx ->
                            val amt = tx.amount.amountToman?.let { MoneyNormalizer.formatToman(it) } ?: tx.amount.originalText
                            listOfNotNull(amt, tx.merchant, TimeFormat.relative(System.currentTimeMillis(), tx.timestamp)).joinToString(" · ")
                        },
                        evidence = AiEvidence(expenses.map { it.sourceMessageId })
                    )
                }
            }
            else -> {
                val hits = search(question).take(6)
                AskAnswer(
                    question = question,
                    answerText = if (hits.isEmpty()) "No matching messages in the current source." else "Most relevant messages:",
                    structuredLines = hits.map { msg ->
                        val who = s.senders[msg.senderId]?.displayName ?: msg.senderId
                        "$who · ${msg.body.lines().first().take(70)}"
                    },
                    evidence = AiEvidence(hits.map { it.id }),
                    freshnessNote = if (s.sourceMode == SourceMode.MOCK) "Answering over Mock SMS" else "Answering over Device SMS"
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
