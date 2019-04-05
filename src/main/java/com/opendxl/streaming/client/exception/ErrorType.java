/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client.exception;

import com.opendxl.streaming.client.Channel;

/**
 * Identifiers corresponding to the different errors that {@link Channel} can throw, e.g.: {@link ConsumerError},
 * {@link TemporaryError}, {@link PermanentError} and {@link StopError}
 */
public enum ErrorType {
    CONSUMER_ERROR,
    PERMANENT_ERROR,
    STOP_ERROR,
    TEMPORARY_ERROR
}
