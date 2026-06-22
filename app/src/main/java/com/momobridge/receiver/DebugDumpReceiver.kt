package com.momobridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.momobridge.data.local.MomoBridgeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DebugDumpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    MomoBridgeDatabase::class.java,
                    "momobridge.db"
                ).build()
                val dao = db.smsTransactionDao()
                val allTxn = dao.observeAll().first()
                db.close()

                val arr = JSONArray()
                for (txn in allTxn) {
                    arr.put(JSONObject().apply {
                        put("id", txn.id)
                        put("network", txn.network)
                        put("reference", txn.reference)
                        put("amount", txn.amount)
                        put("senderName", txn.senderName ?: JSONObject.NULL)
                        put("senderPhone", txn.senderPhone ?: JSONObject.NULL)
                        put("balanceAfter", txn.balanceAfter ?: JSONObject.NULL)
                        put("rawSms", txn.rawSms)
                        put("receivedAt", txn.receivedAt)
                        put("createdAt", txn.createdAt)
                        put("expiresAt", txn.expiresAt)
                        put("confirmedAt", txn.confirmedAt ?: JSONObject.NULL)
                        put("claimedByKeyLabel", txn.claimedByKeyLabel ?: JSONObject.NULL)
                        put("status", txn.status)
                    })
                }
                val root = JSONObject().apply {
                    put("exported_count", allTxn.size)
                    put("exported_at", System.currentTimeMillis())
                    put("transactions", arr)
                }
                val file = File(context.cacheDir, "debug_dump.json")
                file.writeText(root.toString(2))
                android.util.Log.i("DebugDump", "Exported ${allTxn.size} transactions to ${file.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.e("DebugDump", "Failed: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
