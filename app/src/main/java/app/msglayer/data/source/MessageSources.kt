package app.msglayer.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import app.msglayer.data.mock.MockSmsDataset
import app.msglayer.domain.model.Message
import app.msglayer.domain.model.MessageSourceType
import app.msglayer.domain.model.SourceCapabilities
import app.msglayer.domain.source.MessageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MockMessageSource : MessageSource {
    override val id: String = "mock-sms"
    override val displayName: String = "Mock SMS"
    override val capabilities: SourceCapabilities = SourceCapabilities(canRead = true)

    private val messages = MutableStateFlow(MockSmsDataset.messages(System.currentTimeMillis()))

    override suspend fun getMessages(): List<Message> = messages.value

    override fun observeMessages(): Flow<List<Message>> = messages.asStateFlow()

    fun refresh(now: Long = System.currentTimeMillis()) {
        messages.value = MockSmsDataset.messages(now)
    }
}

/**
 * Reads inbox SMS via ContentResolver. Never deletes or modifies device messages.
 */
class SmsMessageSource(
    private val context: Context,
    private val maxMessages: Int = 800
) : MessageSource {
    override val id: String = "android-sms"
    override val displayName: String = "Device SMS"
    override val capabilities: SourceCapabilities = SourceCapabilities(canRead = true)

    private val cache = MutableStateFlow<List<Message>>(emptyList())

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun getMessages(): List<Message> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            cache.value = emptyList()
            return@withContext emptyList()
        }
        val out = mutableListOf<Message>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            var n = 0
            while (cursor.moveToNext() && n < maxMessages) {
                val smsId = cursor.getString(idIdx) ?: continue
                val address = cursor.getString(addrIdx)?.trim().orEmpty().ifBlank { "unknown" }
                val body = cursor.getString(bodyIdx).orEmpty()
                val date = cursor.getLong(dateIdx)
                val read = cursor.getInt(readIdx) == 1
                val senderId = "sms:$address"
                out += Message(
                    id = "sms-$smsId",
                    sourceType = MessageSourceType.SMS,
                    conversationId = "thread-$address",
                    senderId = senderId,
                    body = body,
                    timestamp = date,
                    isRead = read,
                    isOutgoing = false,
                    threadKey = address
                )
                n++
            }
        }
        cache.value = out
        out
    }

    override fun observeMessages(): Flow<List<Message>> = cache.asStateFlow()
}
