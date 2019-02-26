/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to support some facilities for command line options
 */
public class CommandLineUtils {

    private CommandLineUtils() {

    }

    /**
     * This method is invoked when the command line made up by options and argument
     * are ill formed or do not meet options spec. Then , it shows the usage and exit with a error
     *
     * @param parser  The utility capable to show the usage
     * @param message Message Error
     */
    public static void printUsageAndFinish(final OptionParser parser, final String message) {
        try {
            System.err.println("ERROR: " + message);
            parser.printHelpOn(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().exit(1);
    }

    /**
     * This method validate the mandatory arguments for a specific operation
     *
     * @param operationArgument It represents a instance of --operation argument
     * @param parser            The utility capable to show the usage
     * @param options           Represents the set of options in the command line
     */
    public static void validateMandatoryOperationArgs(final CommandLineOperationArgument operationArgument,
                                                      final OptionParser parser, final OptionSet options) {
        if (operationArgument == null) {
            CommandLineUtils.printUsageAndFinish(parser, "[operation argument is unknown]");
        }

        operationArgument.getMandatoryOptions().forEach((option, mandatoryOption) -> {
            if (!options.has(mandatoryOption) && mandatoryOption.defaultValues().isEmpty()) {
                CommandLineUtils.printUsageAndFinish(parser, mandatoryOption.toString() + " is missing for "
                        + operationArgument.getOperationName() + " operation");
            }
        });
    }

    public static void printUsageAndFinish(final String executionResult) {
        System.out.println(executionResult);
        Runtime.getRuntime().exit(0);
    }

    public static Map<String, List<?>>
    getCommandLine(final OptionSet options,
                   final Map<Options, ArgumentAcceptingOptionSpec<String>> mandatoryOptions) {

        Map<OptionSpec<?>, List<?>> optionSpecListMap = options.asMap();

        Map<String, List<?>> result = new HashMap<>();

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Options, ArgumentAcceptingOptionSpec<String>> pair : mandatoryOptions.entrySet()) {
            List<?> objects = optionSpecListMap.get(pair.getValue());
            if (objects != null) {
                final String option = pair.getValue().toString();
                result.put(option.substring(1, option.length() - 1), objects);
            }
        }
        return result;
    }
}
