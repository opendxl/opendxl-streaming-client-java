/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.entity;

import java.util.List;
import java.util.Map;

/**
 * Represent the result of a operation command line
 */
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

    /**
     *
     * @return a object that represents the result according to the specific operation
     */
    public Object getResult() {
        return result;
    }

    public Map<String, List<?>> getOptions() {
        return options;
    }


}
