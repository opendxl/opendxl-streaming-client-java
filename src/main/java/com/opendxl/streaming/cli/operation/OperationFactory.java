/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.operation;

import com.opendxl.streaming.cli.Options;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory class creates instances of --operations arguments . For instance creates a
 * {@link CreateOperation} when command line is --operation create
 *
 */
public class OperationFactory {
    private final  Map<OperationArguments, CommandLineOperation> operationArgumentsFactoryMap = new HashMap<>();

    private final OptionSet options;

    public OperationFactory(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                            final OptionSet options) {
        this.options = options;
        operationArgumentsFactoryMap.put(OperationArguments.LOGIN,
                new LoginOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.CREATE,
                new CreateOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.SUBSCRIBE,
                new SubscribeOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.CONSUME,
                new ConsumeOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.COMMIT,
                new CommitOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.DELETE,
                new DeleteOperation(optionSpecMap, options));
        operationArgumentsFactoryMap.put(OperationArguments.SUBSCRIPTIONS,
                new SubscriptionsOperation(optionSpecMap, options));



    }

    /**
     *
     * @param operationsOpt Operations supported by command line cli
     * @return Command line operation instance
     */
    public CommandLineOperation getOperation(final ArgumentAcceptingOptionSpec<String> operationsOpt) {
        String operationArgument = options.valueOf(operationsOpt);
        return operationArgumentsFactoryMap.get(OperationArguments.fromString(operationArgument));
    }

}
