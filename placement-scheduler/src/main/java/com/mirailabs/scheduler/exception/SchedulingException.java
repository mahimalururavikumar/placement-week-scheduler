package com.mirailabs.scheduler.exception;

public class SchedulingException extends RuntimeException {
    public SchedulingException(String message) {
        super(message);
    }

    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
