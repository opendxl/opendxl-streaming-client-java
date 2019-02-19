/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;

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
     * @param parser The utility capable to show the usage
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
     * @param parser The utility capable to show the usage
     * @param options Represents the set of options in the command line
     */
    public static void validateMandatoryOperationArgs(final CommandLineOperationArgument operationArgument,
                                             final OptionParser parser, final OptionSet options) {
        if (operationArgument == null) {
            CommandLineUtils.printUsageAndFinish(parser, "[operation argument is unknown]");
        }

        operationArgument.getMandatoryOptions().forEach(mandatoryOption -> {
            if (!options.has(mandatoryOption)) {
                CommandLineUtils.printUsageAndFinish(parser, mandatoryOption.toString() + " is missing for "
                        + operationArgument.getOperationName() + " operation");
            }
        });
    }

}
