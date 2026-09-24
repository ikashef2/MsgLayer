package app.msglayer.data.pipeline

import app.msglayer.domain.model.Sender

/** Maps SMS addresses / body hints to known Iranian banks for finance extraction. */
object BankDirectory {

    data class BankHint(
        val accountId: String,
        val bankName: String,
        val match: (address: String, body: String) -> Boolean
    )

    private val banks = listOf(
        BankHint("acc-blu", "Blu") { a, b ->
            a.contains("blu", true) || b.contains("blu", true) || b.contains("بلو")
        },
        BankHint("acc-melli", "Melli") { a, b ->
            a.contains("melli", true) || a.contains("bmelli", true) ||
                b.contains("ملی") || b.contains("melli", true)
        },
        BankHint("acc-mellat", "Mellat") { a, b ->
            a.contains("mellat", true) || b.contains("ملت") || b.contains("mellat", true)
        },
        BankHint("acc-saman", "Saman") { a, b ->
            a.contains("saman", true) || b.contains("سامان")
        },
        BankHint("acc-pasargad", "Pasargad") { a, b ->
            a.contains("pasargad", true) || b.contains("پاسارگاد")
        },
        BankHint("acc-saderat", "Saderat") { a, b ->
            a.contains("saderat", true) || b.contains("صادرات")
        },
        BankHint("acc-tejarat", "Tejarat") { a, b ->
            a.contains("tejarat", true) || b.contains("تجارت")
        }
    )

    fun accountFor(address: String, body: String): BankHint? =
        banks.firstOrNull { it.match(address, body) }

    fun isLikelyBank(address: String, body: String): Boolean =
        accountFor(address, body) != null ||
            body.contains("مانده") || body.contains("balance", true) ||
            body.contains("برداشت") || body.contains("واریز") ||
            body.contains("کارت") || body.contains("iban", true)

    fun isCommercial(address: String, body: String): Boolean {
        val a = address.lowercase()
        val b = body.lowercase()
        return a.contains("irancell") || a.contains("mci") || a.contains("rightel") ||
            b.contains("ایرانسل") || b.contains("تخفیف") || b.contains("promo")
    }

    fun senderFromAddress(address: String, body: String = ""): Sender {
        val bank = accountFor(address, body)
        val commercial = isCommercial(address, body)
        val name = when {
            bank != null -> bank.bankName
            address.startsWith("+98") || address.all { it.isDigit() || it == '+' } -> address
            else -> address
        }
        return Sender(
            id = "sms:$address",
            displayName = name,
            address = address,
            isKnownBank = bank != null || isLikelyBank(address, body),
            isCommercial = commercial
        )
    }
}
