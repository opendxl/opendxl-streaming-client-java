/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the "subscribe" argument for a --operation option
 */
public class SubscribeOperationArgument implements CommandLineOperationArgument {
    private final OptionSet options;
    Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();

    public static final String OPERATION_NAME = OperationArguments.SUBSCRIBE.argumentName;

    /**
     *
     * @param optionSpecMap Options and argument spec
     * @param options parsed command line options and arguments
     */
    public SubscribeOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                      final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.BROKERS, optionSpecMap.get(Options.BROKERS));
        mandatoryOptions.put(Options.TOPIC, optionSpecMap.get(Options.TOPIC));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Options, ArgumentAcceptingOptionSpec<String>> getMandatoryOptions() {
        return mandatoryOptions;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }

    @Override
    public ExecutionResult execute() {

        return null;
    }
}
