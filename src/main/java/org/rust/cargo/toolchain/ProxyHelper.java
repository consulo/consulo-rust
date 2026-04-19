/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.util.net.HttpConfigurable;

import java.net.URI;

public final class ProxyHelper {

    private ProxyHelper() {
    }

    public static void withProxyIfNeeded(GeneralCommandLine cmdLine, HttpConfigurable http) {
        if (http.USE_HTTP_PROXY && !http.PROXY_HOST.isEmpty()) {
            cmdLine.withEnvironment("http_proxy", getProxyUri(http).toString());
        }
    }

    private static URI getProxyUri(HttpConfigurable http) {
        String userInfo = null;
        if (http.PROXY_AUTHENTICATION && http.getProxyLogin() != null && !http.getProxyLogin().isEmpty() && http.getPlainProxyPassword() != null) {
            String login = http.getProxyLogin();
            String password = http.getPlainProxyPassword();
            userInfo = (password != null && !password.isEmpty()) ? login + ":" + password : login;
        }
        return URI.create("http://" + (userInfo != null ? userInfo + "@" : "") + http.PROXY_HOST + ":" + http.PROXY_PORT + "/");
    }
}
