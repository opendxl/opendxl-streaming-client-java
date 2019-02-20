/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

/**
 * Error is a common base class from which other error classes are derived
 */
public class Error extends RuntimeException {

    Error(final String message) {
        super(message);
    }

}

/**
 * Exception raised when an unexpected/unknown (but possibly recoverable) error occurs.
 */
class TemporaryError extends Error {

    TemporaryError(final String message) {
        super(message);
    }

}

/**
 * Exception raised for an operation which would not be expected to succeed even if the operation were retried.
 */
class PermanentError extends Error {

    PermanentError(final String message) {
        super(message);
    }

}

/**
 * Exception raised for an operation which is interrupted due to the channel being stopped.
 */
class StopError extends Error {

    StopError(final String message) {
        super(message);
    }

}

/**
 * Error raised when a channel operation fails due to the associated consumer
 * not being recognized by the streaming service.
 */
class ConsumerError extends TemporaryError {

    ConsumerError(final String message) {
        super(message);
    }

}

/**
 * TODO: add class description for ConsumerProcessorIrrecoverableException
 */
class ConsumerProcessorIrrecoverableException extends Exception {
}

/**
 * TODO: add class description for ConsumerProcessorRecoverableException
 */
class ConsumerProcessorRecoverableException extends Exception {
}
