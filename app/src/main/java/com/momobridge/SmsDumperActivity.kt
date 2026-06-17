package com.momobridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File

class SmsDumperActivity : ComponentActivity() {

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) dumpSms()
            else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            dumpSms()
        } else {
            requestPermission.launch(Manifest.permission.READ_SMS)
        }
    }

    private fun dumpSms() {
        val keywords = listOf("T-CASH", "TCASH", "MTN", "MoMo", "Mobile Money", "Transaction", "GHC", "GH₵")
        val senders = listOf("T-CASH", "TCASH", "MTN", "MTN MoMo")
        val sb = StringBuilder()

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
            null, null,
            "${Telephony.Sms.Inbox.DATE} DESC"
        )

        if (cursor == null) {
            sb.appendLine("ERROR: null cursor returned")
        } else {
            var count = 0
            while (cursor.moveToNext() && count < 100) {
                val address = cursor.getString(0) ?: ""
                val body = cursor.getString(1) ?: ""
                val date = cursor.getLong(2)

                val matchesSender = senders.any { address.contains(it, ignoreCase = true) }
                val matchesBody = keywords.any { body.contains(it, ignoreCase = true) }

                if (matchesSender || matchesBody) {
                    count++
                    sb.appendLine("=== SMS #$count ===")
                    sb.appendLine("From: $address")
                    sb.appendLine("Date: $date")
                    sb.appendLine("Body:")
                    body.lines().forEach { sb.appendLine("  $it") }
                    sb.appendLine()
                }
            }
            cursor.close()
            if (count == 0) {
                sb.appendLine("No matching SMS found (searched last 100)")
                sb.appendLine("Senders searched: $senders")
                sb.appendLine("Keywords searched: $keywords")
            }
        }

        val file = File(cacheDir, "sms_dump.txt")
        file.writeText(sb.toString())
        // Also log it
        android.util.Log.i("SmsDumper", sb.toString())
        finish()
    }
}
