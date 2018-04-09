package com.xjeffrose.xio.SSL;

import static okhttp3.internal.Util.verifyAsIpAddress;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * A certificate and its private key. This can be used on the server side by HTTPS servers, or on
 * the client side to verify those HTTPS servers. A held certificate can also be used to sign other
 * held certificates, as done in practice by certificate authorities.
 */
public final class HeldCertificate {
  public final X509Certificate certificate;
  public final KeyPair keyPair;

  public HeldCertificate(X509Certificate certificate, KeyPair keyPair) {
    this.certificate = certificate;
    this.keyPair = keyPair;
  }

  public static final class Builder {
    static {
      Security.addProvider(new BouncyCastleProvider());
    }

    private final long duration = 1000L * 60 * 60 * 24; // One day.
    private String hostname;
    private List<String> altNames = new ArrayList<>();
    private String serialNumber = "1";
    private KeyPair keyPair;
    private HeldCertificate issuedBy;
    private int maxIntermediateCas;
    private Date notBefore;
    private Date notAfter;

    public Builder serialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
      return this;
    }

    /**
     * Set this certificate's name. Typically this is the URL hostname for TLS certificates. This is
     * the CN (common name) in the certificate. Will be a random string if no value is provided.
     */
    public Builder commonName(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder keyPair(KeyPair keyPair) {
      this.keyPair = keyPair;
      return this;
    }

    /**
     * Set the certificate that signs this certificate. If unset, a self-signed certificate will be
     * generated.
     */
    public Builder issuedBy(HeldCertificate signedBy) {
      this.issuedBy = signedBy;
      return this;
    }

    /**
     * Set this certificate to be a certificate authority, with up to {@code maxIntermediateCas}
     * intermediate certificate authorities beneath it.
     */
    public Builder ca(int maxIntermediateCas) {
      this.maxIntermediateCas = maxIntermediateCas;
      return this;
    }

    public Builder notBefore(Date notBefore) {
      this.notBefore = notBefore;
      return this;
    }

    public Builder notAfter(Date notAfter) {
      this.notAfter = notAfter;
      return this;
    }

    /**
     * Adds a subject alternative name to the certificate. This is usually a hostname or IP address.
     * If no subject alternative names are added that extension will not be used.
     */
    public Builder subjectAlternativeName(String altName) {
      altNames.add(altName);
      return this;
    }

    private void setValidDates() {
      // TODO(CK): Maybe throw to inform the user that they're doing something silly
      if (notBefore == null || notAfter == null) {
        long now = System.currentTimeMillis();
        notBefore = new Date(now);
        notAfter = new Date(now + duration);
      }
    }

    public HeldCertificate build() throws GeneralSecurityException {
      setValidDates();
      // Subject, public & private keys for this certificate.
      KeyPair heldKeyPair = keyPair != null ? keyPair : generateKeyPair();
      X500Principal subject =
          hostname != null
              ? new X500Principal("CN=" + hostname)
              : new X500Principal("CN=" + UUID.randomUUID());

      // Subject, public & private keys for this certificate's signer. It may be self signed!
      KeyPair signedByKeyPair;
      X500Principal signedByPrincipal;
      if (issuedBy != null) {
        signedByKeyPair = issuedBy.keyPair;
        signedByPrincipal = issuedBy.certificate.getSubjectX500Principal();
      } else {
        signedByKeyPair = heldKeyPair;
        signedByPrincipal = subject;
      }

      // Generate & sign the certificate.
      X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
      generator.setSerialNumber(new BigInteger(serialNumber));
      generator.setIssuerDN(signedByPrincipal);
      generator.setNotBefore(notBefore);
      generator.setNotAfter(notAfter);
      generator.setSubjectDN(subject);
      generator.setPublicKey(heldKeyPair.getPublic());
      generator.setSignatureAlgorithm("SHA256WithRSAEncryption");

      if (maxIntermediateCas > 0) {
        generator.addExtension(
            X509Extensions.BasicConstraints, true, new BasicConstraints(maxIntermediateCas));
      }

      if (!altNames.isEmpty()) {
        ASN1Encodable[] encodableAltNames = new ASN1Encodable[altNames.size()];
        for (int i = 0, size = altNames.size(); i < size; i++) {
          String altName = altNames.get(i);
          int tag = verifyAsIpAddress(altName) ? GeneralName.iPAddress : GeneralName.dNSName;
          encodableAltNames[i] = new GeneralName(tag, altName);
        }
        generator.addExtension(
            X509Extensions.SubjectAlternativeName, true, new DERSequence(encodableAltNames));
      }

      X509Certificate certificate =
          generator.generateX509Certificate(signedByKeyPair.getPrivate(), "BC");
      return new HeldCertificate(certificate, heldKeyPair);
    }

    public KeyPair generateKeyPair() throws GeneralSecurityException {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
      keyPairGenerator.initialize(1024, new SecureRandom());
      return keyPairGenerator.generateKeyPair();
    }
  }
}
