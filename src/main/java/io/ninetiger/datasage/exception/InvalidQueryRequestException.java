package io.ninetiger.datasage.exception;

public class InvalidQueryRequestException extends RuntimeException {
    public InvalidQueryRequestException(String message) {
        super(message);
    }
} 