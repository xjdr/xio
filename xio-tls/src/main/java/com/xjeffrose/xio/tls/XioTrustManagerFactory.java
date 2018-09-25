package com.xjeffrose.xio.tls;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import java.security.KeyStore;
import java.security.cert.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class XioTrustManagerFactory extends SimpleTrustManagerFactory {

  private final TrustManagerFactory delegateFactory;
  private final TrustManager[] trustManagers;
  private final X509Certificate[] rootCerts;
  private final boolean allowExpiredClients;

  private class DelegatingTrustManager implements X509TrustManager {
    private final X509TrustManager delegate;

    DelegatingTrustManager(X509TrustManager delegate) {
      this.delegate = delegate;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      if (log.isDebugEnabled()) {
        log.debug("attempting checkClientTrusted for authType {} and chain:", authType);
        for (X509Certificate cert : chain) {
          log.debug("certificate {}", cert);
        }

        try {
          delegate.checkClientTrusted(chain, authType);
          log.debug("checkClientTrusted succeeded.");
        } catch (sun.security.validator.ValidatorException e) {
          log.debug(
              "error certificate {}, error type {}", e.getErrorCertificate(), e.getErrorType());
          if (e.getCause() instanceof sun.security.provider.certpath.SunCertPathBuilderException) {
            log.debug(
                "adjacency list: {}",
                ((sun.security.provider.certpath.SunCertPathBuilderException) e.getCause())
                    .getAdjacencyList());
          }

          log.debug("cause", e.getCause());
          log.debug("validation exception", e);
          if (e.getCause() instanceof CertPathValidatorException) {
            CertPathValidatorException cause = (CertPathValidatorException) e.getCause();
            if (cause.getCause() != null
                && cause.getCause() instanceof CertificateExpiredException) {
              if (allowExpiredClients) {
                log.debug("allowExpiredClients = true, ignoring CertificatedExpiredException");
                return;
              }
            }
          }

          throw e;
        } catch (Exception e) {
          log.debug("checkClientTrusted failed.", e);
          throw e;
        }
      } else {
        try {
          delegate.checkClientTrusted(chain, authType);
        } catch (sun.security.validator.ValidatorException e) {
          if (e.getCause() instanceof CertPathValidatorException) {
            CertPathValidatorException cause = (CertPathValidatorException) e.getCause();
            if (cause.getCause() != null
                && cause.getCause() instanceof CertificateExpiredException) {
              if (allowExpiredClients) {
                log.warn("allowExpiredClients = true, ignoring CertificatedExpiredException");
                return;
              }
            }
          }
        }
      }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      if (log.isDebugEnabled()) {
        log.debug("attempting checkServerTrusted for authType {} and chain:", authType);
        for (X509Certificate cert : chain) {
          log.debug("certificate {}", cert);
        }

        try {
          delegate.checkServerTrusted(chain, authType);
          log.debug("checkServerTrusted succeeded.");
        } catch (sun.security.validator.ValidatorException e) {
          log.debug(
              "error certificate {}, error type {}", e.getErrorCertificate(), e.getErrorType());
          if (e.getCause() instanceof sun.security.provider.certpath.SunCertPathBuilderException) {
            log.debug(
                "adjacency list: {}",
                ((sun.security.provider.certpath.SunCertPathBuilderException) e.getCause())
                    .getAdjacencyList());
          }

          log.debug("cause", e.getCause());
          log.debug("validation exception", e);
          throw e;
        } catch (Exception e) {
          log.debug("checkServerTrusted failed.", e);
          throw e;
        }
      } else {
        delegate.checkServerTrusted(chain, authType);
      }
    }

    // TODO(CK): return our root certs here?
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      if (log.isDebugEnabled()) {
        X509Certificate[] acceptedIssuers = delegate.getAcceptedIssuers();
        log.debug("returning the following certificates from getAcceptedIssuers:");
        for (X509Certificate cert : acceptedIssuers) {
          log.debug("certificate {}", cert);
        }
        return acceptedIssuers;
      } else {
        return delegate.getAcceptedIssuers();
      }
    }
  }

  private TrustManager[] buildTrustManagers(TrustManagerFactory factory) {
    ArrayList<TrustManager> result = new ArrayList<>();
    for (TrustManager tm : factory.getTrustManagers()) {
      if (tm instanceof X509TrustManager) {
        X509TrustManager delegate = (X509TrustManager) tm;
        result.add(new DelegatingTrustManager(delegate));
      } else {
        log.warn("TrustManager is not an instance of X509TrustManager, skipping. {}", tm);
      }
    }

    return result.toArray(new TrustManager[0]);
  }

  XioTrustManagerFactory(
      TrustManagerFactory factory, X509Certificate[] rootCerts, boolean allowExpiredClients) {
    delegateFactory = factory;
    trustManagers = buildTrustManagers(factory);
    this.rootCerts = rootCerts;
    this.allowExpiredClients = allowExpiredClients;
  }

  XioTrustManagerFactory(TrustManagerFactory factory, boolean allowExpiredClients) {
    this(factory, null, allowExpiredClients);
  }

  XioTrustManagerFactory(TrustManagerFactory factory) {
    this(factory, null, false);
  }

  @Override
  protected void engineInit(KeyStore keyStore) throws Exception {}

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {}

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return trustManagers;
  }
}
