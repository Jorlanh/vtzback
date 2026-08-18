package com.votzz.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // Retorna erro 402
public class SubscriptionLockedException extends RuntimeException {
    public SubscriptionLockedException(String message) {
        super(message);
    }
}