/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.util.net;

import java.net.URLConnection;

/**
 * IntelliJ-compat stub. Consulo's networking uses Java's system-wide proxy
 * selection; plugins don't typically manage it themselves.
 */
public final class HttpConfigurable {
    private static final HttpConfigurable INSTANCE = new HttpConfigurable();

    /** Legacy IntelliJ public-field API — kept as plain fields on stub for source compat. */
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
