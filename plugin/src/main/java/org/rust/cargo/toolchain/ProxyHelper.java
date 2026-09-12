/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import consulo.http.HttpProxyManager;
import consulo.process.cmd.GeneralCommandLine;

import java.net.URI;

public final class ProxyHelper {

    private ProxyHelper() {
    }

    public static void withProxyIfNeeded(GeneralCommandLine cmdLine, HttpProxyManager proxy) {
        String host = proxy.getProxyHost();
        if (proxy.isHttpProxyEnabled() && host != null && !host.isEmpty()) {
            cmdLine.withEnvironment("http_proxy", getProxyUri(proxy, host).toString());
        }
    }

    private static URI getProxyUri(HttpProxyManager proxy, String host) {
        String userInfo = null;
        if (proxy.isProxyAuthenticationEnabled()) {
            String login = proxy.getProxyLogin();
            if (login != null && !login.isEmpty()) {
                String password = proxy.getPlainProxyPassword();
                userInfo = (password != null && !password.isEmpty()) ? login + ":" + password : login;
            }
        }
        return URI.create("http://" + (userInfo != null ? userInfo + "@" : "") + host + ":" + proxy.getProxyPort() + "/");
    }
}
