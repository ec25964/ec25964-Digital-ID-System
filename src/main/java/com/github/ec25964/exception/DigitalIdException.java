package com.github.ec25964.exception;

public class DigitalIdException extends RuntimeException {

    public DigitalIdException(String message) {
        super(message);
    }

    public DigitalIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
