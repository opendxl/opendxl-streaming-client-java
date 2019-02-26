package com.opendxl.streaming.cli;

import java.util.List;
import java.util.Map;

public class ExecutionResult {

    private final String code;
    private final Object result;
    private Map<String, List<?>> options;

    public ExecutionResult(final String code, final Object result, final Map<String, List<?>> options) {

        this.code = code;
        this.result = result;
        this.options = options;
    }

    public String getCode() {
        return code;
    }

    public Object getResult() {
        return result;
    }

    public Map<String, List<?>> getOptions() {
        return options;
    }


}
