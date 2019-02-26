/*---------------------------------------------------------------------------*
 * Copyright (c) 2019 McAfee, LLC - All Rights Reserved.                     *
 *---------------------------------------------------------------------------*/

package com.opendxl.streaming.cli;

import com.opendxl.streaming.client.Channel;
import com.opendxl.streaming.client.ChannelAuth;
import com.opendxl.streaming.client.Request;
import junit.extensions.PA;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Helper {

    private Helper() {
    }

    public static ChannelAuth channelAuthFactory(final String authURL,
                                                 final String user,
                                                 final String password,
                                                 final String verifyCertBundle) {
        URL url = null;
        try {
            url = new URL(authURL);
        } catch (MalformedURLException e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }

        return new ChannelAuth(getBaseURL(url),
                user,
                password,
                Optional.of(url.getPath()),
                verifyCertBundle);
    }

    public static String getBaseURL(final URL url) {
        return url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
    }

    public static Optional<Map<String, Object>> configToMap(final String config) {
        final Map<String, Object> map = new HashMap<>();
        try {
            final String[] keyValuePairs = config.split(",");

            for (String pair : keyValuePairs) {
                String[] entry = pair.split("=");
                map.put(entry[0].trim(), entry[1].trim());
            }
        } catch (Exception e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }
        return Optional.of(map);
    }

    public static List<String> topicsToList(final String topics) {
        final List<String> result = new ArrayList<>();
        try {
            return Arrays.asList(topics.split(","));
        } catch (Exception e) {
            CommandLineUtils.printUsageAndFinish(CommandLineInterface.parser, e.getMessage());
        }
        return result;
    }

    public static String getCookie(final Channel channel) {
        final Request request = (Request) PA.getValue(channel, "request");
        final HttpClientContext clientContext = (HttpClientContext) PA.getValue(request, "httpClientContext");
        final List<Cookie> cookies = clientContext.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("AWSALB")) {
                System.out.println(cookie.toString());
                return cookie.getValue();
            }
        }
        return "";
    }

    public static Request configureRequest(ChannelAuth channelAuth, URL url, final String cookie) {
        final Request request = PA.instantiate(Request.class, Helper.getBaseURL(url), channelAuth);
        final HttpClientContext clientContext = (HttpClientContext) PA.getValue(request, "httpClientContext");
        final BasicCookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(new BasicClientCookie("AWSALB", cookie));
        clientContext.setCookieStore(cookieStore);
        return request;
    }
}
