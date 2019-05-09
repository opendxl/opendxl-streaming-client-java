/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.client;

import com.opendxl.streaming.client.exception.PermanentError;

/**
 * The {@link HttpProxySettings} class collects all http proxy settings in a single object.
 */
public class HttpProxySettings {

    private boolean enabled;
    private String url;
    private int port;
    private String username;
    private String password;

    /**
     *
     * @param enabled whether the Http Proxy settings are applicable or not.
     * @param url http proxy url. Mandatory parameter, it may not be null nor empty.
     * @param port http proxy port. Mandatory parameter, it may not be zero or lower than zero.
     * @param username username to provide to http proxy.
     * @param password user password.
     * @throws PermanentError if http proxy url is null or empty or if port number is not greater than zero.
     */
    public HttpProxySettings(final boolean enabled, final String url, final int port, final String username,
                             final String password) throws PermanentError {

        if (url == null || url.isEmpty()) {
            throw new PermanentError("Http Proxy url may not be null or empty");
        }

        if (port <= 0) {
            throw new PermanentError("Http Proxy port must be greater than zero. Provided value was: " + port);
        }

        this.enabled = enabled;
        this.url = url;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Indicate whether Http Proxy is enabled or disabled
     *
     * @return true if http proxy is enabled
     *         false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the Http Proxy URL
     *
     * @return http proxy url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the Http Proxy TCP port number
     *
     * @return http proxy port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the username required to access the Http Proxy
     *
     * @return http proxy username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password required to access the Http Proxy
     *
     * @return http proxy password
     */
    public String getPassword() {
        return password;
    }

}
