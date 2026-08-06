package com.intellij.openapi.application;
public abstract class TransactionGuard {
    public static TransactionGuard getInstance() { return null; }
    public abstract void submitTransaction(Object parentDisposable, Runnable transaction);
    public static void submitTransactionLater(Object parentDisposable, Runnable transaction) { transaction.run(); }
}
