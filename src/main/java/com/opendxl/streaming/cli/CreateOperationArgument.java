/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the "create" argument for a --operation option
 */
public class CreateOperationArgument implements CommandLineOperationArgument {

    /**
     * The operation name
     */
    public static final String OPERATION_NAME = OperationArguments.CREATE.argumentName;

    List<ArgumentAcceptingOptionSpec<String>> mandatoryOptions = new ArrayList<>();


    public CreateOperationArgument(final Map<Options, ArgumentAcceptingOptionSpec<String>> optionSpecMap) {
        mandatoryOptions.add(optionSpecMap.get(Options.BROKERS));
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