/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the "create" argument for a --operation option
 */
public class CreateOperationArgument implements CommandLineOperationArgument {

    /**
     * The operation name
     */
    public static final String OPERATION_NAME = OperationArguments.CREATE.argumentName;


    private final OptionSet options;

    Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new HashMap<>();


    /**
     *  Constructor
     *
     * @param optionSpecMap Options and argument spec
     * @param options parsed command line options and arguments
     */
    public CreateOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                   final OptionSet options) {
        this.options = options;
        mandatoryOptions.put(Options.BROKERS, optionSpecMap.get(Options.BROKERS));
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