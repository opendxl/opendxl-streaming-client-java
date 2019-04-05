/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.operation;

/**
 * Arguments of --operation command line option
 */
public enum OperationArguments {

    LOGIN("login"),
    CREATE("create"),
    SUBSCRIBE("subscribe"),
    CONSUME("consume"),
    COMMIT("commit"),
    DELETE("delete"),
    SUBSCRIPTIONS("subscriptions");


    OperationArguments(final String argumentName) {
        this.argumentName = argumentName;
    }
    String argumentName;

    /**
     * Return a OperationArgument enumerated based on a string
     *
     * @param operationArgumentValue A string value to be converted in a OperationArguments enumerated
     * @return a OperationArguments enumerated
     */
    public static OperationArguments fromString(final String operationArgumentValue) {
        for (OperationArguments operationArgumentEnum : OperationArguments.values()) {
            if (operationArgumentEnum.argumentName.equalsIgnoreCase(operationArgumentValue)) {
                return operationArgumentEnum;
            }
        }
        return null;
    }
}
