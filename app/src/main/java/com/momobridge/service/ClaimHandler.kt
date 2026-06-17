package com.momobridge.service

import com.momobridge.data.local.SmsTransactionEntity
import com.momobridge.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

data class ClaimResult(
    val confirmed: Boolean,
    val message: String,
    val reference: String? = null,
    val amount: Double? = null,
    val network: String? = null,
    val senderName: String? = null,
    val claimedByKeyLabel: String? = null
)

@Singleton
class ClaimHandler @Inject constructor(
    private val repository: TransactionRepository
) {
    private fun isExpired(txn: SmsTransactionEntity): Boolean {
        if (!repository.isExpiryEnabled()) return false
        return txn.status == SmsTransactionEntity.EXPIRED || txn.expiresAt < System.currentTimeMillis()
    }

    suspend fun handleClaim(reference: String, amount: Double, claimedByKeyLabel: String? = null): ClaimResult {
        if (reference.isBlank()) {
            return ClaimResult(false, "Reference is required")
        }

        val txn = repository.findByReference(reference.trim())

        if (txn == null) {
            return ClaimResult(false, "Transaction not found")
        }

        if (txn.status == SmsTransactionEntity.CONFIRMED) {
            return ClaimResult(
                confirmed = false,
                message = "Transaction already confirmed",
                reference = txn.reference,
                amount = txn.amount,
                network = txn.network,
                senderName = txn.senderName
            )
        }

        if (isExpired(txn)) {
            repository.updateStatusByReference(txn.reference, SmsTransactionEntity.EXPIRED)
            return ClaimResult(
                confirmed = false,
                message = "Transaction reference has expired",
                reference = txn.reference,
                amount = txn.amount,
                network = txn.network,
                senderName = txn.senderName
            )
        }

        if (kotlin.math.abs(txn.amount - amount) > 0.01) {
            return ClaimResult(
                confirmed = false,
                message = "Amount mismatch. Expected GH₵${"%.2f".format(txn.amount)}",
                reference = txn.reference,
                amount = txn.amount,
                network = txn.network,
                senderName = txn.senderName
            )
        }

        val success = if (claimedByKeyLabel != null) {
            repository.confirmTransactionWithLabel(txn.reference, claimedByKeyLabel)
        } else {
            repository.confirmTransaction(txn.reference)
        }

        if (success) {
            return ClaimResult(
                confirmed = true,
                message = "Payment confirmed",
                reference = txn.reference,
                amount = txn.amount,
                network = txn.network,
                senderName = txn.senderName,
                claimedByKeyLabel = claimedByKeyLabel
            )
        }

        return ClaimResult(false, "Could not confirm transaction")
    }
}
