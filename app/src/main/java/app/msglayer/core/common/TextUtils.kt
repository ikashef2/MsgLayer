package app.msglayer.core.common

object PersianText {
    private val persianDigits = charArrayOf('\u06F0', '\u06F1', '\u06F2', '\u06F3', '\u06F4', '\u06F5', '\u06F6', '\u06F7', '\u06F8', '\u06F9')
    private val arabicDigits = charArrayOf('\u0660', '\u0661', '\u0662', '\u0663', '\u0664', '\u0665', '\u0666', '\u0667', '\u0668', '\u0669')

    fun toEnglishDigits(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            val p = persianDigits.indexOf(ch)
            val a = arabicDigits.indexOf(ch)
            when {
                p >= 0 -> append('0' + p)
                a >= 0 -> append('0' + a)
                else -> append(ch)
            }
        }
    }

    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { ch ->
            if (ch in '0'..'9') append(persianDigits[ch - '0']) else append(ch)
        }
    }

    fun normalizeWhitespace(input: String): String =
        input.replace('\u200c', ' ').replace(Regex("\\s+"), " ").trim()

    fun containsPersian(input: String): Boolean =
        input.any { it in '\u0600'..'\u06FF' }
}

object MoneyNormalizer {
    private val amountRegex = Regex(
        """([\d\u06F0-\u06F9\u0660-\u0669][\d\u06F0-\u06F9\u0660-\u0669\u066C,\.\s]*)\s*(\u062A\u0648\u0645\u0627\u0646|\u062A\u0648\u0645\u0646|\u0631\u06CC\u0627\u0644|rial|toman|irr|\bT\b)?""",
        RegexOption.IGNORE_CASE
    )

    data class ParsedMoney(
        val rawNumber: Long,
        val unitHint: app.msglayer.domain.model.CurrencyUnit,
        val originalText: String,
        val toman: Long?,
        val conversionConfident: Boolean
    )

    fun parse(text: String): ParsedMoney? {
        val normalized = PersianText.toEnglishDigits(text)
            .replace('\u066C', ',')
            .replace('\u060C', ',')
        val match = amountRegex.find(normalized) ?: return null
        val numberPart = match.groupValues[1]
            .replace(",", "")
            .replace(" ", "")
            .replace(".", "")
        val raw = numberPart.toLongOrNull() ?: return null
        val unitRaw = match.groupValues.getOrNull(2)?.lowercase().orEmpty()
        val unit = when {
            unitRaw.contains("\u0631\u06CC\u0627\u0644") || unitRaw.contains("rial") || unitRaw == "irr" ->
                app.msglayer.domain.model.CurrencyUnit.RIAL
            unitRaw.contains("\u062A\u0648\u0645\u0627\u0646") || unitRaw.contains("\u062A\u0648\u0645\u0646") || unitRaw.contains("toman") || unitRaw == "t" ->
                app.msglayer.domain.model.CurrencyUnit.TOMAN
            else -> app.msglayer.domain.model.CurrencyUnit.UNKNOWN
        }
        val (toman, confident) = when (unit) {
            app.msglayer.domain.model.CurrencyUnit.TOMAN -> raw to true
            app.msglayer.domain.model.CurrencyUnit.RIAL -> (raw / 10) to true
            app.msglayer.domain.model.CurrencyUnit.UNKNOWN -> null to false
        }
        return ParsedMoney(
            rawNumber = raw,
            unitHint = unit,
            originalText = match.value.trim(),
            toman = toman,
            conversionConfident = confident
        )
    }

    fun formatToman(amount: Long, persianDigits: Boolean = false): String {
        val grouped = "%,d".format(amount).replace(',', '\u066C')
        val withUnit = "$grouped T"
        return if (persianDigits) PersianText.toPersianDigits(withUnit) else withUnit
    }
}

object TimeFormat {
    fun relative(now: Long, then: Long): String {
        val diff = now - then
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "yesterday"
            days < 14 -> "$days days ago"
            else -> "${days / 7} weeks ago"
        }
    }

    fun greeting(nowMillis: Long): String {
        val hour = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
            .get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
        }
    }
}