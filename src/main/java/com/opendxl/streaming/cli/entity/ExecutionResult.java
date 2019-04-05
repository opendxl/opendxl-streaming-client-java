/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.entity;

import com.opendxl.streaming.client.Channel;

import java.util.List;
import java.util.Map;

/**
 * Represent the result of a operation command line
 */
public class ExecutionResult {

    /**
     * HTTP Status Code of the HTTP Response received when command is executed.
     */
    private final String code;
    /**
     * <p>Object showing the data returned by the executed command. The data varies according to the executed Operation,
     * i.e.:</p>
     * <p>LoginOperation result consists of the obtained token</p>
     * <p>CreateOperation result consists of the obtained ConsumerId and the Cookie</p>
     * <p>SubscribeOperation result is empty since the {@link Channel#subscribe(List)} response has no content</p>
     * <p>SubscriptionsOperation result is the list of the subscribed topic names</p>
     * <p>ConsumeOperation result is a list of consumer records</p>
     * <p>CommitOperation result is empty since the {@link Channel#commit()} operation response has no content</p>
     * <p>DeleteOperation result is empty since the {@link Channel#delete()} operation response has no content</p>
     */
    private final Object result;
    /**
     * List of options used for a specific operation command line
     */
    private Map<String, List<?>> options;

    /**
     * Result of a command line operation.
     *
     * @param code HTTP Status Code of the HTTP Response received when command is executed.
     * @param result output data returned by the executed command line operation. The type of output data varies
     *               depending on the executed operation, i.e.: create operation returns a ConsumerId and a Cookie,
     *               consume operation returns a list of consumer records while subscribe, commit and delete operations
     *               do not return any data.
     * @param options list of input options entered for the operation
     */
    public ExecutionResult(final String code, final Object result, final Map<String, List<?>> options) {

        this.code = code;
        this.result = result;
        this.options = options;
    }

    /**
     *
     * @return HTTP Status code of the HTTP Response
     */
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

    /**
     *
     * @return list of input options entered for the operation
     */
    public Map<String, List<?>> getOptions() {
        return options;
    }


}
