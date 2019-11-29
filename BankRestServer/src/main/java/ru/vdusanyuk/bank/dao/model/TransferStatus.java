package ru.vdusanyuk.bank.dao.model;

/**
 * enum for Transfer opration statuses
 */
public enum TransferStatus {
    DRAFT,
    PENDING,
    PROCESSED,
    ERROR;

    public boolean isProcessed() {
       return PROCESSED.equals(this) || ERROR.equals(this);
    }

    public boolean isPending() {
        return PENDING.equals(this);
    }
}
