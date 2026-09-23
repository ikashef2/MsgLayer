package app.msglayer.data.mock

import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageSourceType
import app.msglayer.domain.model.Sender

/**
 * Realistic Persian-heavy seed dataset for offline prototyping.
 * Timestamps are relative to [now].
 */
object MockSmsDataset {

    fun senders(): List<Sender> = listOf(
        Sender("s-blu", "Blu Bank", "Blu", isKnownBank = true),
        Sender("s-melli", "Bank Melli", "BMELLI", isKnownBank = true),
        Sender("s-mellat", "Bank Mellat", "BankMellat", isKnownBank = true),
        Sender("s-irancell", "Irancell", "989120000000", isCommercial = true),
        Sender("s-digikala", "Digikala", "Digikala"),
        Sender("s-dental", "Tehran Dental Clinic", "02191000000"),
        Sender("s-ima", "Ima", "+989121111111"),
        Sender("s-reza", "Reza", "+989122222222"),
        Sender("s-ali", "Ali", "+989133333333"),
        Sender("s-epfund", "EPFund", "EPFund"),
        Sender("s-snapp", "Snapp", "Snapp"),
        Sender("s-bargh", "Electricity Bill", "Tavanir"),
        Sender("s-otp-google", "Google", "Google")
    )

    fun messages(now: Long): List<Message> {
        fun ago(h: Long = 0, m: Long = 0, d: Long = 0) =
            now - (((d * 24 + h) * 60 + m) * 60_000)

        // Unicode escapes keep source ASCII-safe while preserving Persian SMS content.
        val bluOld = "\u0628\u0644\u0648\n\u0628\u0631\u062F\u0627\u0634\u062A: \u06F2\u066C\u06F2\u06F2\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646\n\u0645\u0627\u0646\u062F\u0647: \u06F1\u066C\u06F8\u06F0\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646"
        val bluNew = "\u0628\u0644\u0648\n\u0628\u0631\u062F\u0627\u0634\u062A: \u06F4\u06F2\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646\n\u0645\u0627\u0646\u062F\u0647: \u06F1\u066C\u06F3\u06F8\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646\n\u0641\u0631\u0648\u0634\u06AF\u0627\u0647: Snapp"
        val melliBal = "\u0628\u0627\u0646\u06A9 \u0645\u0644\u06CC\n\u062D\u0633\u0627\u0628 *\u06F4\u06F5\u06F6\u06F7\n\u0645\u0627\u0646\u062F\u0647: \u06F2\u066C\u06F3\u06F0\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646"
        val mellatOld = "Bank Mellat\nBalance: 1,200,000 T\nCard *8899"
        val mellatNew = "\u0628\u0627\u0646\u06A9 \u0645\u0644\u062A\n\u0645\u0627\u0646\u062F\u0647: \u06F7\u06F8\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646\n\u06A9\u0627\u0631\u062A *\u06F8\u06F8\u06F9\u06F9"
        val mellatFail = "\u0628\u0627\u0646\u06A9 \u0645\u0644\u062A\n\u067E\u0631\u062F\u0627\u062E\u062A \u0646\u0627\u0645\u0648\u0641\u0642\n\u0645\u0628\u0644\u063A: \u06F5\u06F0\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646"
        val otp1 = "\u06A9\u062F \u062A\u0627\u06CC\u06CC\u062F \u0634\u0645\u0627: \u06F4\u06F5\u06F6\u06F7\u06F8\u06F9\nGoogle"
        val otp2 = "Blu OTP: 882211\nvalid 5 min"
        val otp3 = "\u0631\u0645\u0632 \u06F1\u06F2\u06F3\u06F4\u06F5\u06F6 \u0628\u0627\u0646\u06A9 \u0645\u0644\u06CC"
        val otp4 = "Digikala code: 771122"
        val promo1 = "\u0627\u06CC\u0631\u0627\u0646\u0633\u0644\n\u067E\u06A9\u06CC\u062C \u0648\u06CC\u0698\u0647 \u0627\u06CC\u0646\u062A\u0631\u0646\u062A! \u062A\u0627 \u06F5\u06F0\u066A \u062A\u062E\u0641\u06CC\u0641"
        val promo2 = "Irancell: 20GB gift if you recharge today"
        val promo3 = "\u0627\u06CC\u0631\u0627\u0646\u0633\u0644: \u0645\u06A9\u0627\u0644\u0645\u0647 \u0647\u0627\u06CC \u062C\u062F\u06CC\u062F \u0631\u0627 \u0627\u0632 \u062F\u0633\u062A \u0646\u062F\u0647\u06CC\u062F!"
        val digiShip = "\u062F\u06CC\u062C\u06CC\u06A9\u0627\u0644\u0627\n\u0645\u0631\u0633\u0648\u0644\u0647 \u0634\u0645\u0627 \u062A\u062D\u0648\u06CC\u0644 \u0634\u062F\n\u06A9\u062F \u067E\u06CC\u06AF\u06CC\u0631\u06CC: DK-998877\n\u0648\u0635\u0648\u0644: \u0627\u0645\u0631\u0648\u0632"
        val digiOut = "Digikala: package out for delivery. Tracking DK-998877"
        val dental = "Tehran Dental Clinic\nReminder: Thu Sep 24, 17:30\nDr. Hosseini"
        val imaQ = "\u0633\u0644\u0627\u0645! \u062C\u0645\u0639\u0647 \u062C\u0645\u0639\u0647 \u0628\u0631\u0627\u06CC \u062C\u0645\u0639\u0647 \u0645\u06CC\u0631\u06CC\u0645\u061F Are we still going Friday?"
        val rezaInv = "Can you send the invoice for EPFund?"
        val aliCard = "\u0634\u0645\u0627\u0631\u0647 \u06A9\u0627\u0631\u062A \u062C\u062F\u06CC\u062F\u0645:\n6037-9911-2233-4455"
        val bill = "\u0642\u0628\u0636 \u0628\u0631\u0642\n\u0645\u0628\u0644\u063A: \u06F8\u06F5\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646\n\u0633\u0631\u0631\u0633\u06CC\u062F: \u0641\u0631\u062F\u0627"
        val epfund = "EPFund: Q3 report ready. Please review by Monday."
        val snappPay = "Snapp\n\u067E\u0631\u062F\u0627\u062E\u062A \u0645\u0648\u0641\u0642\n\u06F1\u06F2\u06F0\u066C\u06F0\u06F0\u06F0 \u062A\u0648\u0645\u0627\u0646"
        val meetingOld = "Meeting is at 4 PM."
        val meetingNew = "Sorry, changed to 5:30."
        val address = "Address: Vanak Sq, No. 12, Unit 4"
        val spam = "WIN PRIZE NOW click http://spam.example"

        return listOf(
            msg("m-blu-old", "c-blu", "s-blu", bluOld, ago(d = 1, h = 2)),
            msg("m-blu-new", "c-blu", "s-blu", bluNew, ago(m = 12)),
            msg("m-melli-bal", "c-melli", "s-melli", melliBal, ago(d = 1, h = 5)),
            msg("m-mellat-old", "c-mellat", "s-mellat", mellatOld, ago(d = 8)),
            msg("m-mellat-new", "c-mellat", "s-mellat", mellatNew, ago(d = 8, h = -1)), // still stale-ish relative narrative; use 8d
            msg("m-mellat-fail", "c-mellat", "s-mellat", mellatFail, ago(h = 6)),
            msg("m-otp-1", "c-google", "s-otp-google", otp1, ago(h = 3)),
            msg("m-otp-2", "c-blu-otp", "s-blu", otp2, ago(h = 5)),
            msg("m-otp-3", "c-melli-otp", "s-melli", otp3, ago(h = 8)),
            msg("m-otp-4", "c-digi-otp", "s-digikala", otp4, ago(h = 10)),
            msg("m-promo-1", "c-irancell", "s-irancell", promo1, ago(h = 2)),
            msg("m-promo-2", "c-irancell", "s-irancell", promo2, ago(h = 14)),
            msg("m-promo-3", "c-irancell", "s-irancell", promo3, ago(d = 1)),
            msg("m-promo-4", "c-irancell", "s-irancell", promo1, ago(d = 2)),
            msg("m-promo-5", "c-irancell", "s-irancell", promo2, ago(d = 2, h = 3)),
            msg("m-promo-6", "c-irancell", "s-irancell", promo3, ago(d = 3)),
            msg("m-promo-7", "c-irancell", "s-irancell", promo1, ago(d = 3, h = 4)),
            msg("m-promo-8", "c-irancell", "s-irancell", promo2, ago(d = 4)),
            msg("m-digi-1", "c-digikala", "s-digikala", digiShip, ago(h = 9)),
            msg("m-digi-2", "c-digikala", "s-digikala", digiOut, ago(h = 1)),
            msg("m-dental", "c-dental", "s-dental", dental, ago(d = 3, h = 4)),
            msg("m-ima", "c-ima", "s-ima", imaQ, ago(h = 3), read = false),
            msg("m-reza", "c-reza", "s-reza", rezaInv, ago(d = 1), read = false),
            msg("m-ali", "c-ali", "s-ali", aliCard, ago(h = 7)),
            msg("m-bill", "c-bargh", "s-bargh", bill, ago(h = 20)),
            msg("m-epfund", "c-epfund", "s-epfund", epfund, ago(d = 1, h = 2)),
            msg("m-snapp", "c-snapp", "s-snapp", snappPay, ago(d = 4)),
            msg("m-meet-1", "c-ima", "s-ima", meetingOld, ago(d = 2, h = 6)),
            msg("m-meet-2", "c-ima", "s-ima", meetingNew, ago(d = 2, h = 5)),
            msg("m-addr", "c-reza", "s-reza", address, ago(d = 5)),
            msg("m-spam", "c-spam", "s-irancell", spam, ago(d = 1, h = 8)),
            msg("m-personal", "c-ali", "s-ali", "Figma link: https://figma.com/file/abc123/design", ago(d = 6))
        ).map {
            // Fix mellat new timestamp to 8 days ago (stale balance case)
            if (it.id == "m-mellat-new") it.copy(timestamp = ago(d = 8)) else it
        }
    }

    private fun msg(
        id: String,
        conversationId: String,
        senderId: String,
        body: String,
        timestamp: Long,
        read: Boolean = true
    ) = Message(
        id = id,
        sourceType = MessageSourceType.MOCK,
        conversationId = conversationId,
        senderId = senderId,
        body = body,
        timestamp = timestamp,
        isRead = read
    )
}
