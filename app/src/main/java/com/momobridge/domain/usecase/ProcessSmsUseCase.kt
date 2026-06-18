package com.momobridge.domain.usecase

import com.momobridge.data.repository.TransactionRepository
import com.momobridge.domain.model.ParsedTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessSmsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(parsed: ParsedTransaction, status: String = "PENDING"): Long {
        return repository.saveTransaction(parsed, status)
    }
}
