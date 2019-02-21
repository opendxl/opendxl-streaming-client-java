/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory class creates instances of --operations arguments . For instance creates a
 * {@link CreateOperationArgument} when command line is --operation create
 *
 */
public class OperationArgumentFactory {
    private final  Map<OperationArguments, CommandLineOperationArgument> operationArgumentsFactoryMap = new HashMap<>();

    private final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap;
    private final OptionSet options;

    public OperationArgumentFactory(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                    final OptionSet options) {
        this.options = options;
        this.optionSpecMap = optionSpecMap;
        operationArgumentsFactoryMap.put(OperationArguments.LOGIN,
                new LoginOperationArgument(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.CREATE,
                new CreateOperationArgument(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.SUBSCRIBE,
                new SubscribeOperationArgument(optionSpecMap, options));
    }

    public CommandLineOperationArgument getOperation(final ArgumentAcceptingOptionSpec<String> operationsOpt) {
        String operationArgument = options.valueOf(operationsOpt);
        return operationArgumentsFactoryMap.get(OperationArguments.fromString(operationArgument));
    }

}
