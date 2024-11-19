package io.ninetiger.datasage.exception;

public class QueryProcessingException extends RuntimeException {
    public QueryProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
} 