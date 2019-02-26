/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli.operation;

import com.opendxl.streaming.cli.entity.ExecutionResult;
import com.opendxl.streaming.cli.Options;
import joptsimple.ArgumentAcceptingOptionSpec;

import java.util.Map;

/**
 * Interface for all --operation arguments
 */
public interface CommandLineOperation {

    /**
     *
     * @return List if mandatory options for the specific operation
     */
    Map<Options, ArgumentAcceptingOptionSpec<String>> getMandatoryOptions();

    /**
     *
     * @return the operation name
     */
    String getOperationName();

    /**
     * Execute the operation
     *
     * @return Execution result
     */
    ExecutionResult execute();
}
