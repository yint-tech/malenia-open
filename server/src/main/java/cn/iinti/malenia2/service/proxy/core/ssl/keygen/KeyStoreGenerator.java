package cn.iinti.malenia2.service.proxy.core.ssl.keygen;


import cn.iinti.malenia2.service.base.trace.Recorder;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_friendlyName;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_localKeyId;

/**
 * Dynamic generate self signed certificate for mitm proxy, signed by private key in the root certificate, with SAN names.
 * JDK do not have an open api for building X509 certificate, so we use  Bouncy Castle here.
 */
@Slf4j
public class KeyStoreGenerator {

    public static final String certAliasName = "Malenia2 app";
    private final X509Certificate rootCert;
    private final RSAPrivateCrtKeyParameters privateKeyParameters;

    private final SecureRandom secureRandom;
    private final Random random;
    private final JcaX509ExtensionUtils jcaX509ExtensionUtils;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    public KeyStoreGenerator(byte[] keyData, char[] rootKeyStorePassword) throws Exception {

        KeyStore rootKeyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = new ByteArrayInputStream(keyData)) {
            rootKeyStore.load(input, rootKeyStorePassword);
        }

        Enumeration<String> aliases = rootKeyStore.aliases();
        String alias = aliases.nextElement();
        log.debug("Loading CA certificate/private by alias {}", alias);

        Key key = rootKeyStore.getKey(alias, rootKeyStorePassword);
        requireNonNull(key, "Specified key of the KeyStore not found!");
        RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) key;
        privateKeyParameters = getPrivateKeyParameters(privateCrtKey);
        // and get the certificate

        rootCert = (X509Certificate) rootKeyStore.getCertificate(alias);
        requireNonNull(rootCert, "Specified certificate of the KeyStore not found!");
        log.debug("Successfully loaded CA key and certificate. CA DN is {}", rootCert.getSubjectDN().getName());
        rootCert.verify(rootCert.getPublicKey());
        log.debug("Successfully verified CA certificate with its own public key.");

        secureRandom = new SecureRandom();
        random = new Random();
        jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
    }

    public BigInteger getRootCertSN() {
        return rootCert.getSerialNumber();
    }

    /**
     * return pub certificate byte data
     *
     * @param pem if false, return crt data; if true, return pem encoded data
     */
    public byte[] exportRootCert(boolean pem) throws CertificateEncodingException {
        byte[] data = rootCert.getEncoded();
        if (!pem) {
            return data;
        }
        return ("-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder().encodeToString(data) +
                "\n-----END CERTIFICATE-----\n").getBytes(StandardCharsets.US_ASCII);
    }

    private RSAPrivateCrtKeyParameters getPrivateKeyParameters(RSAPrivateCrtKey privateCrtKey) {
        return new RSAPrivateCrtKeyParameters(privateCrtKey.getModulus(),
                privateCrtKey.getPublicExponent(),
                privateCrtKey.getPrivateExponent(),
                privateCrtKey.getPrimeP(), privateCrtKey.getPrimeQ(), privateCrtKey.getPrimeExponentP(),
                privateCrtKey.getPrimeExponentQ(),
                privateCrtKey.getCrtCoefficient());
    }


    public PrivateKeyAndCertChain generateCertChain(String host, int validityDays, Recorder recorder) throws Exception {
        return generateCertChain(Collections.singletonList(host), validityDays, recorder);
    }

    public PrivateKeyAndCertChain generateCertChain(List<String> hosts, int validityDays, Recorder recorder) throws Exception {
        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalStateException("empty host config");
        }

        List<GeneralName> generalNames = Lists.newLinkedList();
        for (String host : hosts) {
            int hostType = Networks.getHostType(host);
            int tag = (hostType == Networks.HOST_TYPE_IPV4 || hostType == Networks.HOST_TYPE_IPV6) ? GeneralName.iPAddress : GeneralName.dNSName;
            try {
                generalNames.add(new GeneralName(tag, host));
            } catch (Exception e) {
                recorder.recordEvent("can not get host GeneralName: " + host, e);
            }
        }

        if (generalNames.isEmpty()) {
            generalNames.add(new GeneralName(GeneralName.dNSName, "malenia.iinti.cn"));
        }


        // generate the key pair for the new certificate
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, secureRandom);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        Calendar calendar = Calendar.getInstance();
        // in case client time behind server time
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date startDate = calendar.getTime();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date expireDate = calendar.getTime();

        String appDName = "CN=malenia, OU=iinti, O=iinti, L=Chengdu, ST=Chengdu, C=CN";
        X500Name subject = new X500Name(appDName);
        ASN1ObjectIdentifier sigOID = PKCSObjectIdentifiers.sha256WithRSAEncryption;
        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, DERNull.INSTANCE);

        V3TBSCertificateGenerator generator = new V3TBSCertificateGenerator();
        generator.setSerialNumber(new ASN1Integer(random.nextLong() + System.currentTimeMillis()));
        generator.setIssuer(getSubject(rootCert));
        generator.setSubject(subject);
        generator.setSignature(sigAlgId);
        generator.setSubjectPublicKeyInfo(getPublicKeyInfo(publicKey));
        generator.setStartDate(new Time(startDate));
        generator.setEndDate(new Time(expireDate));

        // Set SubjectAlternativeName
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
        extensionsGenerator.addExtension(Extension.subjectAlternativeName, false, () -> {
            ASN1EncodableVector nameVector = new ASN1EncodableVector();
            generalNames.forEach(nameVector::add);
            return GeneralNames.getInstance(new DERSequence(nameVector)).toASN1Primitive();
        });
        Extensions x509Extensions = extensionsGenerator.generate();
        generator.setExtensions(x509Extensions);

        TBSCertificate tbsCertificateStructure = generator.generateTBSCertificate();
        byte[] data = toBinaryData(tbsCertificateStructure);
        byte[] signatureData = signData(sigOID, data, privateKeyParameters, secureRandom);

        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(tbsCertificateStructure);
        asn1EncodableVector.add(sigAlgId);
        asn1EncodableVector.add(new DERBitString(signatureData));

        DERSequence derSequence = new DERSequence(asn1EncodableVector);
        Certificate certificate = Certificate.getInstance(derSequence);
        X509CertificateObject clientCertificate = new X509CertificateObject(certificate);
        log.debug("Verifying certificate for correct signature with CA public key");
        clientCertificate.verify(rootCert.getPublicKey());
        clientCertificate.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString("Certificate for malenia App"));
        clientCertificate.setBagAttribute(pkcs_9_at_localKeyId,
                jcaX509ExtensionUtils.createSubjectKeyIdentifier(publicKey));

        return new PrivateKeyAndCertChain(privateKey, new X509Certificate[]{clientCertificate, rootCert});
    }

    private static byte[] signData(ASN1ObjectIdentifier sigOID, byte[] data,
                                   RSAPrivateCrtKeyParameters privateKeyParameters,
                                   SecureRandom secureRandom) throws Exception {
        PrivateKey caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(getKeySpec(privateKeyParameters));
        Signature signature = Signature.getInstance(sigOID.getId());
        signature.initSign(caPrivateKey, secureRandom);
        signature.update(data);
        return signature.sign();
    }

    private static byte[] toBinaryData(TBSCertificate tbsCertificateStructure) throws IOException {
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            DEROutputStream derOutputStream = new DEROutputStream(bos);
            try {
                derOutputStream.writeObject(tbsCertificateStructure);
                data = bos.toByteArray();
            } finally {
                derOutputStream.close();
            }
        }
        return data;
    }

    private static X500Name getSubject(X509Certificate certificate) throws IOException, CertificateEncodingException {
        TBSCertificateStructure tbsCert = TBSCertificateStructure.getInstance(
                ASN1Primitive.fromByteArray(certificate.getTBSCertificate()));
        return tbsCert.getSubject();
    }

    private static SubjectPublicKeyInfo getPublicKeyInfo(PublicKey publicKey) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(publicKey.getEncoded());
             ASN1InputStream asn1InputStream = new ASN1InputStream(bis)) {
            return SubjectPublicKeyInfo.getInstance(asn1InputStream.readObject());
        }
    }

    private static RSAPrivateCrtKeySpec getKeySpec(RSAPrivateCrtKeyParameters privateKeyParameters) {
        return new RSAPrivateCrtKeySpec(privateKeyParameters.getModulus(),
                privateKeyParameters.getPublicExponent(), privateKeyParameters.getExponent(),
                privateKeyParameters.getP(), privateKeyParameters.getQ(),
                privateKeyParameters.getDP(), privateKeyParameters.getDQ(), privateKeyParameters.getQInv());
    }


}
