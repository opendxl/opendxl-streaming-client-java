package com.opendxl.streaming.cli;

public class ExecutionResult {



    private final String errorCode;
    private final String message;

    public ExecutionResult(final String errorCode, final String message) {

        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
