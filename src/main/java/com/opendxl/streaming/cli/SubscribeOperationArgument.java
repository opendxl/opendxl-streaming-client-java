/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the "subscribe" argument for a --operation option
 */
public class SubscribeOperationArgument implements CommandLineOperationArgument {
    private final OptionSet options;
    List<ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new ArrayList<>();

    public static final String OPERATION_NAME = OperationArguments.SUBSCRIBE.argumentName;

    public SubscribeOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap,
                                      final OptionSet options) {
        this.options = options;
        mandatoryOptions.add(optionSpecMap.get(Options.BROKERS));
        mandatoryOptions.add(optionSpecMap.get(Options.TOPIC));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ArgumentAcceptingOptionSpec<String>> getMandatoryOptions() {
        return mandatoryOptions;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getOperationName() {
        return OPERATION_NAME;
    }
}
