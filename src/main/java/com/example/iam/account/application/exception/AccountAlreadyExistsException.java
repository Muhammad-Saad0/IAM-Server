package com.example.iam.account.application.exception;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException() {
    }

    public AccountAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
