/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.util.net;

import java.net.URLConnection;

/**
 * Proxy configuration holder: no proxy is ever reported, and connections are opened
 * directly.
 */
public final class HttpConfigurable {
    private static final HttpConfigurable INSTANCE = new HttpConfigurable();

    public boolean USE_HTTP_PROXY = false;
    public boolean USE_PROXY_PAC = false;
    public boolean PROXY_AUTHENTICATION = false;
    public boolean PROXY_TYPE_IS_SOCKS = false;
    public String PROXY_HOST = "";
    public int PROXY_PORT = 0;

    private HttpConfigurable() {}

    public static HttpConfigurable getInstance() { return INSTANCE; }

    public String getProxyLogin() { return null; }
    public String getPlainProxyPassword() { return null; }

    public void prepareURL(String url) {}
    public URLConnection openConnection(String location) throws java.io.IOException {
        return new java.net.URL(location).openConnection();
    }
}
