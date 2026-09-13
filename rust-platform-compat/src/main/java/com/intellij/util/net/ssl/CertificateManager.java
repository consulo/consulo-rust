package com.intellij.util.net.ssl;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Supplies the SSL context used for outgoing connections; returns the JVM default. */
public final class CertificateManager {
    private static final CertificateManager INSTANCE = new CertificateManager();
    private CertificateManager() {}
    public static CertificateManager getInstance() { return INSTANCE; }
    public SSLContext getSslContext() {
        try { return SSLContext.getDefault(); } catch (Exception e) { return null; }
    }
}
