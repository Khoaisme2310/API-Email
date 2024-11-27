package com.example.demo.utils;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.*;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.security.SecureRandom;
import java.security.Security;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import java.util.Date;
import com.example.demo.entity.PgpKeyEntity;
import java.util.Base64;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import java.math.BigInteger;
import java.util.Iterator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;


public class PgpUtils {
    private static final String PASSPHRASE = "";
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }



    public static String encodePublicKey(PGPPublicKey publicKey) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }

    public static String encodePrivateKey(PGPPrivateKey privateKey, PGPPublicKey publicKey, String userId) 
            throws Exception {
        PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider()
            .get(HashAlgorithmTags.SHA1);
        
        PBESecretKeyEncryptor encryptor = new BcPBESecretKeyEncryptorBuilder(
            SymmetricKeyAlgorithmTags.AES_256, 
            sha1Calc)
            .build(PASSPHRASE.toCharArray());

        PGPSecretKey secretKey = new PGPSecretKey(
            privateKey,
            publicKey,
            sha1Calc,
            false,
            encryptor
        );

        byte[] encoded = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }





    public static byte[] decryptData(InputStream encryptedData, PGPPrivateKey privateKey) throws Exception {
        PGPObjectFactory factory = new PGPObjectFactory(
                PGPUtil.getDecoderStream(encryptedData),
                new JcaKeyFingerprintCalculator()
        );

        Object object = factory.nextObject();

        if (object instanceof PGPEncryptedDataList) {
            PGPEncryptedDataList encryptedDataList = (PGPEncryptedDataList) object;
            PGPPublicKeyEncryptedData encryptedDataObject =
                    (PGPPublicKeyEncryptedData) encryptedDataList.get(0);

            InputStream clearStream = encryptedDataObject.getDataStream(
                    new JcePublicKeyDataDecryptorFactoryBuilder()
                            .setProvider("BC")
                            .build(privateKey)
            );

            return Streams.readAll(clearStream);
        }

        throw new IllegalArgumentException("Invalid encrypted data format");
    }

    public static boolean verifySignature(byte[] originalMessage, byte[] signatureData, PGPPublicKey publicKey) throws Exception {
        InputStream sigInputStream = new ByteArrayInputStream(signatureData);
        PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(sigInputStream), new JcaKeyFingerprintCalculator());
        PGPSignatureList signatureList = (PGPSignatureList) factory.nextObject();
        PGPSignature signature = signatureList.get(0);

        signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
        signature.update(originalMessage);

        return signature.verify();
    }

    // Thêm phương thức đọc public key từ byte array
    public static PGPPublicKey readPublicKeyFromByteArray(byte[] keyBytes) throws IOException, PGPException {
        InputStream in = PGPUtil.getDecoderStream(new ByteArrayInputStream(keyBytes));
        PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(in, new JcaKeyFingerprintCalculator());
        
        PGPPublicKeyRing publicKeyRing = pgpPub.getKeyRings().next();
        return publicKeyRing.getPublicKey();
    }

    // Thêm phương thức đọc private key từ InputStream
    public static PGPKeyPair readPrivateKey(InputStream input) throws IOException, PGPException {
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(input), 
            new JcaKeyFingerprintCalculator()
        );
        
        PGPSecretKeyRing secretKeyRing = pgpSec.getKeyRings().next();
        PGPSecretKey secretKey = secretKeyRing.getSecretKey();
        
        PGPPrivateKey privateKey = secretKey.extractPrivateKey(
            new JcePBESecretKeyDecryptorBuilder()
                .setProvider("BC")
                .build(new char[0]) 
        );
        
        return new PGPKeyPair(secretKey.getPublicKey(), privateKey);
    }

    public static PGPPublicKey decodePublicKey(String publicKeyString) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
        try (InputStream in = new ByteArrayInputStream(publicKeyBytes)) {
            PGPPublicKeyRing publicKeyRing = new PGPPublicKeyRing(in, new JcaKeyFingerprintCalculator());
            return publicKeyRing.getPublicKey();
        }
    }

    public static String encryptMessage(String message, PGPPublicKey publicKey) throws Exception {
        try (ByteArrayOutputStream encOut = new ByteArrayOutputStream()) {
            PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256)
                    .setWithIntegrityPacket(true)
                    .setSecureRandom(new SecureRandom())
                    .setProvider("BC")
            );

            encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"));

            try (OutputStream out = encGen.open(encOut, new byte[1 << 16])) {
                PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
                
                try (OutputStream compressedOut = comData.open(out)) {
                    PGPLiteralDataGenerator litData = new PGPLiteralDataGenerator();
                    try (OutputStream literalOut = litData.open(compressedOut,
                            PGPLiteralData.BINARY,
                            "msg.txt",
                            message.getBytes().length,
                            new Date())) {
                        literalOut.write(message.getBytes());
                    }
                }
            }

            return Base64.getEncoder().encodeToString(encOut.toByteArray());
        }
    }


    public static String decrypt(String encryptedMessage, PGPPrivateKey privateKey, PGPPublicKey publicKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
        
        try (InputStream in = new ByteArrayInputStream(encryptedBytes)) {
            PGPObjectFactory pgpF = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
            PGPEncryptedDataList enc;

            Object o = pgpF.nextObject();
            if (o instanceof PGPEncryptedDataList) {
                enc = (PGPEncryptedDataList) o;
            } else {
                enc = (PGPEncryptedDataList) pgpF.nextObject();
            }

            Iterator<PGPEncryptedData> it = enc.getEncryptedDataObjects();
            PGPPublicKeyEncryptedData pbe = null;

            while (it.hasNext()) {
                PGPEncryptedData encData = it.next();
                if (encData instanceof PGPPublicKeyEncryptedData) {
                    pbe = (PGPPublicKeyEncryptedData) encData;
                    break;
                }
            }

            if (pbe == null) {
                throw new IllegalArgumentException("No encrypted data found");
            }

            InputStream clear = pbe.getDataStream(
                new JcePublicKeyDataDecryptorFactoryBuilder()
                    .setProvider("BC")
                    .build(privateKey));

            PGPObjectFactory plainFact = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
            Object message = plainFact.nextObject();

            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream(), new JcaKeyFingerprintCalculator());
                message = pgpFact.nextObject();
            }

            if (message instanceof PGPLiteralData) {
                PGPLiteralData ld = (PGPLiteralData) message;
                InputStream unc = ld.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int ch;
                while ((ch = unc.read()) >= 0) {
                    out.write(ch);
                }
                return new String(out.toByteArray());
            }

            throw new IllegalArgumentException("Message is not a simple encrypted file");
        }
    }

    public static String sign(String message, PGPPrivateKey privateKey, PGPPublicKey publicKey) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(
                new JcaPGPContentSignerBuilder(publicKey.getAlgorithm(), PGPUtil.SHA256)
                    .setProvider("BC"));

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            try (OutputStream literalOut = new ArmoredOutputStream(out)) {
                byte[] messageBytes = message.getBytes();
                signatureGenerator.update(messageBytes);
                signatureGenerator.generate().encode(literalOut);
            }

            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }

    public static PGPPrivateKey decodePrivateKey(String privateKeyString) throws Exception {
        try {
            // Decode base64 string to byte array
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            
            // Create input stream
            try (InputStream in = new ByteArrayInputStream(privateKeyBytes);
                 InputStream decoderStream = PGPUtil.getDecoderStream(in)) {
                
                // Read the secret key ring
                PGPSecretKeyRing secretKeyRing = new PGPSecretKeyRing(decoderStream, new JcaKeyFingerprintCalculator());
                
                // Get the secret key
                PGPSecretKey pgpSecKey = secretKeyRing.getSecretKey();
                
                // Extract the private key
                return pgpSecKey.extractPrivateKey(
                    new JcePBESecretKeyDecryptorBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(new char[0]));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error decoding private key: " + e.getMessage(), e);
        }
    }

    // Thêm phương thức để generate key pair
    public static PGPKeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        return new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());
    }

    // Thêm phương thức để encode private key
    public static String encodePrivateKey(PGPKeyPair keyPair) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PGPSecretKey secretKey = new PGPSecretKey(
                PGPSignature.DEFAULT_CERTIFICATION,
                keyPair,
                "test@example.com",
                null,
                null,
                new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), PGPUtil.SHA1),
                new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256)
                    .setProvider("BC")
                    .build(new char[0])
            );

            try (ArmoredOutputStream armoredOut = new ArmoredOutputStream(out)) {
                secretKey.encode(armoredOut);
            }

            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }

    public static boolean verifySignature(String message, String signature, PGPPublicKey publicKey) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(signature);
        
        try (InputStream in = new ByteArrayInputStream(signatureBytes)) {
            try (InputStream armoredIn = PGPUtil.getDecoderStream(in)) {
                PGPObjectFactory pgpFact = new PGPObjectFactory(armoredIn, new JcaKeyFingerprintCalculator());
                PGPSignatureList sigList = (PGPSignatureList) pgpFact.nextObject();
                PGPSignature sig = sigList.get(0);
                
                sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), publicKey);
                sig.update(message.getBytes());
                
                return sig.verify();
            }
        }
    }

}
