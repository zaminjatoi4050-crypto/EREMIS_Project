package com.eremis.controller;

import com.eremis.model.Transaction;
import com.eremis.service.TransactionService;

import java.math.BigDecimal;
import java.util.List;

public class TransactionController {
    private final TransactionService transactionService = new TransactionService();

    public Transaction submitPurchase(int propertyId, String bankName, String accountNumber, BigDecimal amount) {
        return transactionService.initiatePurchase(propertyId, bankName, accountNumber, amount);
    }

    public List<Transaction> getPendingTransactions() {
        return transactionService.getPendingTransactions();
    }

    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    public List<Transaction> getMyTransactions(int buyerId) {
        return transactionService.getMyTransactions(buyerId);
    }

    public Transaction approve(int transactionId, int adminUserId) {
        return transactionService.approve(transactionId, adminUserId);
    }

    public Transaction reject(int transactionId, int adminUserId, String reason) {
        return transactionService.reject(transactionId, adminUserId, reason);
    }
}