package org.apache.tomcat.util.net;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPathParameters;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.tomcat.util.file.ConfigFileLoader;
import org.apache.tomcat.util.net.jsse.ExposePEMFile;
import org.apache.tomcat.util.net.jsse.JSSEKeyManager;
import org.apache.tomcat.util.res.StringManager;

public abstract class AbstractSSLUtilBase extends SSLUtilBase {

    private static final StringManager sm = StringManager.getManager(AbstractSSLUtilBase.class);

    protected String[] enabledProtocols;
    protected String[] enabledCiphers;

    protected final SSLHostConfig sslHostConfig;

    protected AbstractSSLUtilBase(SSLHostConfigCertificate certificate) {
        super(certificate);
        this.sslHostConfig = this.certificate.getSSLHostConfig();
        this.enabledProtocols = super.getEnabledProtocols();
        List<String> ciphers = new ArrayList<>();
        if (sslHostConfig.getCiphers() != null && !"".equals(sslHostConfig.getCiphers()) && !"HIGH:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!kRSA".equals(sslHostConfig.getCiphers())) {
            for (String cipher : sslHostConfig.getCiphers().split(",")) {
                ciphers.add(cipher);
            }
        }
        if (ciphers.isEmpty()) {
            for (String cipher : getImplementedCiphers()) {
                ciphers.add(cipher);
            }
        } else {
            ciphers.retainAll(getImplementedCiphers());
        }
        this.enabledCiphers = ciphers.toArray(new String[ciphers.size()]);
        getLog().info("implementedProtocols : " + org.apache.tomcat.util.buf.StringUtils.join(getImplementedProtocols()));
        getLog().info("enabledProtocols : " + org.apache.tomcat.util.buf.StringUtils.join(this.enabledProtocols));
        getLog().info("implementedCiphers : " + org.apache.tomcat.util.buf.StringUtils.join(getImplementedCiphers()));
        getLog().info("enabledCiphers : " + org.apache.tomcat.util.buf.StringUtils.join(this.enabledCiphers));
    }

    @Override
    public String[] getEnabledProtocols() {
        return this.enabledProtocols;
    }

    @Override
    public String[] getEnabledCiphers() {
        return this.enabledCiphers;
    }

    @Override
    public KeyManager[] getKeyManagers() throws Exception {
        String keyAlias = certificate.getCertificateKeyAlias();
        String algorithm = sslHostConfig.getKeyManagerAlgorithm();
        String keyPass = certificate.getCertificateKeyPassword();
        // This has to be here as it can't be moved to SSLHostConfig since the
        // defaults vary between JSSE and OpenSSL.
        if (keyPass == null) {
            keyPass = certificate.getCertificateKeystorePassword();
        }

        KeyStore ks = certificate.getCertificateKeystore();
        KeyStore ksUsed = ks;

        /*
         * Use an in memory key store where possible. For PEM format keys and
         * certificates, it allows them to be imported into the expected format. For
         * Java key stores with PKCS8 encoded keys (e.g. JKS files), it enables Tomcat
         * to handle the case where multiple keys exist in the key store, each with a
         * different password. The KeyManagerFactory can't handle that so using an in
         * memory key store with just the required key works around that. Other keys
         * stores (hardware, MS, etc.) will be used as is.
         */

        char[] keyPassArray = keyPass.toCharArray();

        if (ks == null) {
            if (certificate.getCertificateFile() == null) {
                throw new IOException(sm.getString("jsse.noCertFile"));
            }

            ExposePEMFile privateKeyFile = new ExposePEMFile(SSLHostConfig.adjustRelativePath(certificate.getCertificateKeyFile() != null ? certificate.getCertificateKeyFile() : certificate.getCertificateFile()), keyPass);
            ExposePEMFile certificateFile = new ExposePEMFile(SSLHostConfig.adjustRelativePath(certificate.getCertificateFile()));

            Collection<Certificate> chain = new ArrayList<>();
            chain.addAll(certificateFile.getCertificates());
            if (certificate.getCertificateChainFile() != null) {
                ExposePEMFile certificateChainFile = new ExposePEMFile(SSLHostConfig.adjustRelativePath(certificate.getCertificateChainFile()));
                chain.addAll(certificateChainFile.getCertificates());
            }

            if (keyAlias == null) {
                keyAlias = "tomcat";
            }

            // Switch to in-memory key store
            ksUsed = KeyStore.getInstance("JKS");
            ksUsed.load(null, null);
            ksUsed.setKeyEntry(keyAlias, privateKeyFile.getPrivateKey(), keyPass.toCharArray(), chain.toArray(new Certificate[chain.size()]));
        } else {
            if (keyAlias != null && !ks.isKeyEntry(keyAlias)) {
                throw new IOException(sm.getString("jsse.alias_no_key_entry", keyAlias));
            } else if (keyAlias == null) {
                Enumeration<String> aliases = ks.aliases();
                if (!aliases.hasMoreElements()) {
                    throw new IOException(sm.getString("jsse.noKeys"));
                }
                while (aliases.hasMoreElements() && keyAlias == null) {
                    keyAlias = aliases.nextElement();
                    if (!ks.isKeyEntry(keyAlias)) {
                        keyAlias = null;
                    }
                }
                if (keyAlias == null) {
                    throw new IOException(sm.getString("jsse.alias_no_key_entry", (Object) null));
                }
            }

            Key k = ks.getKey(keyAlias, keyPassArray);
            if (k != null && "PKCS#8".equalsIgnoreCase(k.getFormat())) {
                // Switch to in-memory key store
                String provider = certificate.getCertificateKeystoreProvider();
                if (provider == null) {
                    ksUsed = KeyStore.getInstance(certificate.getCertificateKeystoreType());
                } else {
                    ksUsed = KeyStore.getInstance(certificate.getCertificateKeystoreType(), provider);
                }
                ksUsed.load(null, null);
                ksUsed.setKeyEntry(keyAlias, k, keyPassArray, ks.getCertificateChain(keyAlias));
            }
            // Non-PKCS#8 key stores will use the original key store
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ksUsed, keyPassArray);

        KeyManager[] kms = kmf.getKeyManagers();

        // Only need to filter keys by alias if there are key managers to filter
        // and the original key store was used. The in memory key stores only
        // have a single key so don't need filtering
        if (kms != null && ksUsed == ks) {
            String alias = keyAlias;
            // JKS keystores always convert the alias name to lower case
            if ("JKS".equals(certificate.getCertificateKeystoreType())) {
                alias = alias.toLowerCase(Locale.ENGLISH);
            }
            for (int i = 0; i < kms.length; i++) {
                kms[i] = new JSSEKeyManager((X509KeyManager) kms[i], alias);
            }
        }

        return kms;
    }

    @Override
    public TrustManager[] getTrustManagers() throws Exception {

        String className = sslHostConfig.getTrustManagerClassName();
        if (className != null && className.length() > 0) {
            ClassLoader classLoader = getClass().getClassLoader();
            Class<?> clazz = classLoader.loadClass(className);
            if (!(TrustManager.class.isAssignableFrom(clazz))) {
                throw new InstantiationException(sm.getString("jsse.invalidTrustManagerClassName", className));
            }
            Object trustManagerObject = clazz.getConstructor().newInstance();
            TrustManager trustManager = (TrustManager) trustManagerObject;
            return new TrustManager[] { trustManager };
        }

        TrustManager[] tms = null;

        KeyStore trustStore = sslHostConfig.getTruststore();
        if (trustStore != null) {
            checkTrustStoreEntries(trustStore);
            String algorithm = sslHostConfig.getTruststoreAlgorithm();
            String crlf = sslHostConfig.getCertificateRevocationListFile();
            boolean revocationEnabled = sslHostConfig.getRevocationEnabled();

            if ("PKIX".equalsIgnoreCase(algorithm)) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                CertPathParameters params = getParameters(crlf, trustStore, revocationEnabled);
                ManagerFactoryParameters mfp = new CertPathTrustManagerParameters(params);
                tmf.init(mfp);
                tms = tmf.getTrustManagers();
            } else {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(trustStore);
                tms = tmf.getTrustManagers();
                if (crlf != null && crlf.length() > 0) {
                    throw new CRLException(sm.getString("jsseUtil.noCrlSupport", algorithm));
                }
                // Only warn if the attribute has been explicitly configured
                if (sslHostConfig.isCertificateVerificationDepthConfigured()) {
                    getLog().warn(sm.getString("jsseUtil.noVerificationDepth", algorithm));
                }
            }
        }

        return tms;
    }

    /**
     * Load the collection of CRLs.
     * 
     * @param crlf
     *            The path to the CRL file.
     * @return the CRLs collection
     * @throws IOException
     *             Error reading CRL file
     * @throws CRLException
     *             CRL error
     * @throws CertificateException
     *             Error processing certificate
     */
    protected Collection<? extends CRL> getCRLs(String crlf) throws IOException, CRLException, CertificateException {

        Collection<? extends CRL> crls = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream is = ConfigFileLoader.getInputStream(crlf)) {
                crls = cf.generateCRLs(is);
            }
        } catch (IOException iex) {
            throw iex;
        } catch (CRLException crle) {
            throw crle;
        } catch (CertificateException ce) {
            throw ce;
        }
        return crls;
    }

    /**
     * Return the initialization parameters for the TrustManager. Currently, only
     * the default <code>PKIX</code> is supported.
     *
     * @param crlf
     *            The path to the CRL file.
     * @param trustStore
     *            The configured TrustStore.
     * @param revocationEnabled
     *            Should the JSSE provider perform revocation checks? Ignored if
     *            {@code crlf} is non-null. Configuration of revocation checks are
     *            expected to be via proprietary JSSE provider methods.
     * @return The parameters including the CRLs and TrustStore.
     * @throws Exception
     *             An error occurred
     */
    protected CertPathParameters getParameters(String crlf, KeyStore trustStore, boolean revocationEnabled) throws Exception {

        PKIXBuilderParameters xparams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
        if (crlf != null && crlf.length() > 0) {
            Collection<? extends CRL> crls = getCRLs(crlf);
            CertStoreParameters csp = new CollectionCertStoreParameters(crls);
            CertStore store = CertStore.getInstance("Collection", csp);
            xparams.addCertStore(store);
            xparams.setRevocationEnabled(true);
        } else {
            xparams.setRevocationEnabled(revocationEnabled);
        }
        xparams.setMaxPathLength(sslHostConfig.getCertificateVerificationDepth());
        return xparams;
    }

    @Override
    public void configureSessionContext(SSLSessionContext sslSessionContext) {
        sslSessionContext.setSessionCacheSize(sslHostConfig.getSessionCacheSize());
        sslSessionContext.setSessionTimeout(sslHostConfig.getSessionTimeout());
    }

    private void checkTrustStoreEntries(KeyStore trustStore) throws Exception {
        Enumeration<String> aliases = trustStore.aliases();
        if (aliases != null) {
            Date now = new Date();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (trustStore.isCertificateEntry(alias)) {
                    Certificate cert = trustStore.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        try {
                            ((X509Certificate) cert).checkValidity(now);
                        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                            String msg = sm.getString("jsseUtil.trustedCertNotValid", alias, ((X509Certificate) cert).getSubjectDN(), e.getMessage());
                            if (getLog().isDebugEnabled()) {
                                getLog().debug(msg, e);
                            } else {
                                getLog().warn(msg);
                            }
                        }
                    } else {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug(sm.getString("jsseUtil.trustedCertNotChecked", alias));
                        }
                    }
                }
            }
        }
    }

}
