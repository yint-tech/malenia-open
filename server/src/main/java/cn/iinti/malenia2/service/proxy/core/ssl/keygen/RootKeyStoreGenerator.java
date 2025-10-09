package cn.iinti.malenia2.service.proxy.core.ssl.keygen;



import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * Generate ca root key store.
 * First, generate one public-private key pair, then create a X509 certificate, including the generated public key.
 * JDK do not have an open api for building X509 certificate, so we use  Bouncy Castle here.
 *
 * @author Liu Dong
 */
public class RootKeyStoreGenerator {

    public static final String DEFAULT_ROOT_KEY_STORE_NAME = "Malenia.p12";
    public static final char[] DEFAULT_ROOT_KEY_STORE_PASSWORD = "hjkprfvbnml".toCharArray();
    public static final String DEFAULT_ROOT_KEY_STORE_ENTRY_NAME = "Malenia";

    @SuppressWarnings("all")
    public static void main(String[] args) throws Exception {
        // 生成默认的根证书
        String file = RootKeyStoreGenerator.class.getClassLoader().getResource("banner.txt")
                .getFile();

        byte[] bytes = getInstance().generate(DEFAULT_ROOT_KEY_STORE_PASSWORD, 3600);
        File dir = new File(file).getParentFile();

        FileUtils.writeByteArrayToFile(new File(dir, DEFAULT_ROOT_KEY_STORE_NAME), bytes);
    }

    private static final RootKeyStoreGenerator instance = new RootKeyStoreGenerator();

    private RootKeyStoreGenerator() {
    }

    public static RootKeyStoreGenerator getInstance() {
        return instance;
    }

    /**
     * Generate a root ca key store.
     * The key store is stored by p#12 format, X.509 Certificate encoded in der format
     *
     * @return the key store binary data
     * @throws Exception
     */
    public byte[] generate(char[] password, int validityDays) throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, secureRandom);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        Security.addProvider(new BouncyCastleProvider());

        String appDName = "CN=YInt, OU=YInt, O=YInt, L=ChengDu, ST=SiChuan, C=CN";
        X500Name issuerName = new X500Name(appDName);
        X500Name subjectName = new X500Name(appDName);
        Calendar calendar = Calendar.getInstance();
        // in case client time behind server time
        calendar.add(Calendar.DAY_OF_YEAR, -100);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date endDate = calendar.getTime();


        byte[] encoded = publicKey.getEncoded();
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded));
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(issuerName,
                BigInteger.valueOf(secureRandom.nextLong() + System.currentTimeMillis()),
                startDate, endDate,
                subjectName,
                subjectPublicKeyInfo
        );

        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(publicKey));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment
                | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
        builder.addExtension(Extension.keyUsage, false, usage);

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth);
        purposes.add(KeyPurposeId.id_kp_clientAuth);
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

        X509Certificate cert = signCertificate(builder, privateKey);
        cert.checkValidity(new Date());
        cert.verify(publicKey);

        X509Certificate[] chain = new X509Certificate[]{cert};

        keyStore.setEntry(DEFAULT_ROOT_KEY_STORE_ENTRY_NAME, new KeyStore.PrivateKeyEntry(privateKey, chain),
                new KeyStore.PasswordProtection(password));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            keyStore.store(bos, password);
            return bos.toByteArray();
        }
    }

    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException {
        try (ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()))) {
            ASN1Sequence seq = (ASN1Sequence) is.readObject();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
        }
    }

    private static X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder,
                                                   PrivateKey signedWithPrivateKey)
            throws OperatorCreationException, CertificateException {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(signedWithPrivateKey);
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(signer));
    }
}
