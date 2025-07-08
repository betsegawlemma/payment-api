package org.kifiya.paymentapi.model;

public enum ProviderStatus {
    SUCCESS,
    TIMEOUT,
    ERROR,
    OTHER;

    public static ProviderStatus from(String raw) {
        try {
            return ProviderStatus.valueOf(raw);
        } catch (IllegalArgumentException iae) {
            return OTHER;
        }
    }

    public boolean isRetryable() {
        return this == TIMEOUT || this == ERROR;
    }
}
