/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

/**
 * HttpStatusCodes defines the HTTP codes which the Databus Consumer Service can return.
 */
public enum HttpStatusCodes {
    OK(200),
    NO_CONTENT(204),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    INTERNAL_SERVER_ERROR(500);

    private final int code;

    HttpStatusCodes(int statusCode) {
        this.code = statusCode;
    }

    /**
     * Checks whether a status code is successful one
     *
     * @param statusCode an HTTP Response Status Code
     * @return true if status code belongs to 2xx Success range
     *         false otherwise
     */
    public static boolean isSuccess(final int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Get the HttpStatus object for a HTTP Status Code
     *
     * @param statusCode an HTTP Response Status Code
     * @return HttpStatusCodes object corresponding to the statusCode value
     */
    public static HttpStatusCodes getHttpStatus(final int statusCode) {

        for (HttpStatusCodes httpStatus : values()) {
            if (statusCode == httpStatus.code) {
                return httpStatus;
            }
        }

        return null;

    }

    /**
     * Get the integer value corresponding to the HTTP Status Code
     *
     * @return HTTP code value
     */
    public int getCode() {
        return code;
    }

}
