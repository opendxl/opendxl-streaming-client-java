/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.exception.PermanentError;
import com.opendxl.streaming.client.exception.TemporaryError;
import org.apache.http.HttpRequest;

/**
 * Interface to be implemented by Authentication classes responsible for adding the Authorization header to channel
 * requests.
 */
public interface ChannelAuth {

    /**
     * Adds the http header with the Authorization token to the given http request
     *
     * @param httpRequest request where to add the Authorization header
     * @throws TemporaryError if an unexpected (but possibly recoverable) authentication error occurs for the request.
     * @throws PermanentError if the request fails due to the user not being authenticated successfully or if the user
     * is unauthorized to make the request or if a non-recoverable authentication error occurs for the request.
     */
    void authenticate(HttpRequest httpRequest) throws PermanentError, TemporaryError;

    /**
     * Purge any credentials cached from a previous authentication.
     */
    void reset();

}
