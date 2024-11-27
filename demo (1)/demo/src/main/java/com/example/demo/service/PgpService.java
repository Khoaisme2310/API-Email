package com.example.demo.service;

import com.example.demo.utils.PgpUtils;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.entity.PgpKeyEntity;
import java.util.Optional;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;



@Service
public class PgpService {
    private final PgpKeyManagementService keyManagementService;
    private static final Logger logger = LoggerFactory.getLogger(PgpService.class);

    @Autowired
    public PgpService(PgpKeyManagementService keyManagementService) {
        this.keyManagementService = keyManagementService;
    }

    public String encryptMessage(String email, String message) throws Exception {
        logger.info("Starting encryption for email: {}", email);
        
        // Check if email is null or empty
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        // Find key in database
        Optional<PgpKeyEntity> keyEntityOpt = keyManagementService.getKeyPairByEmail(email);
        
        logger.info("Key found in database: {}", keyEntityOpt.isPresent());
        
        if (keyEntityOpt.isEmpty()) {
            // List all emails in database for debugging
            List<PgpKeyEntity> allKeys = keyManagementService.findAll();
            logger.info("Available emails in database: {}", 
                allKeys.stream().map(PgpKeyEntity::getEmail).collect(Collectors.toList()));
            
            throw new IllegalArgumentException("No public key found for email: " + email);
        }

        PgpKeyEntity keyEntity = keyEntityOpt.get();
        logger.info("Found key entity with ID: {}", keyEntity.getId());

        try {
            // Decode public key
            String publicKeyStr = keyEntity.getPublicKey();
            logger.info("Public key length: {}", publicKeyStr.length());
            
            PGPPublicKey publicKey = PgpUtils.decodePublicKey(publicKeyStr);
            logger.info("Successfully decoded public key");

            // Encrypt message
            String encryptedMessage = PgpUtils.encryptMessage(message, publicKey);
            logger.info("Successfully encrypted message. Length: {}", encryptedMessage.length());
            
            return encryptedMessage;
        } catch (Exception e) {
            logger.error("Error during encryption process", e);
            throw e;
        }
    }

    public String signMessage(String email, String message) throws Exception {
        try {
            logger.info("Starting signing process for email: {}", email);
            
            // Get key pair from database
            PgpKeyEntity keyEntity = keyManagementService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No key found for email: " + email));

            logger.info("Found key pair for email: {}", email);
            logger.debug("Private key length: {}", keyEntity.getPrivateKey().length());
            logger.debug("Public key length: {}", keyEntity.getPublicKey().length());

            try {
                // Decode keys
                PGPPrivateKey privateKey = PgpUtils.decodePrivateKey(keyEntity.getPrivateKey());
                PGPPublicKey publicKey = PgpUtils.decodePublicKey(keyEntity.getPublicKey());
                
                logger.info("Successfully decoded keys");

                // Sign message
                String signature = PgpUtils.sign(message, privateKey, publicKey);
                logger.info("Successfully signed message. Signature length: {}", signature.length());
                
                return signature;
                
            } catch (Exception e) {
                logger.error("Error during key decoding or signing: {}", e.getMessage(), e);
                throw new RuntimeException("Error during signing process", e);
            }
        } catch (Exception e) {
            logger.error("Signing failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String decryptMessage(String email, String encryptedMessage) throws Exception {
        try {
            logger.info("Starting decryption for email: {}", email);
            
            // Trim và loại bỏ whitespace
            encryptedMessage = encryptedMessage.trim();
            
            // Kiểm tra chuỗi input
            if (encryptedMessage == null || encryptedMessage.isEmpty()) {
                throw new IllegalArgumentException("Encrypted message cannot be empty");
            }
            
            logger.info("Encrypted message length: {}", encryptedMessage.length());

            // Tìm key trong database
            PgpKeyEntity keyEntity = keyManagementService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No key found for email: " + email));

            // Decode private key
            String privateKeyStr = keyEntity.getPrivateKey();
            String publicKeyStr = keyEntity.getPublicKey();
            
            logger.info("Found keys for email: {}", email);

            try {
                PGPPrivateKey privateKey = PgpUtils.readPrivateKey(new ByteArrayInputStream(Base64.getDecoder().decode(privateKeyStr))).getPrivateKey();
                PGPPublicKey publicKey = PgpUtils.decodePublicKey(publicKeyStr);
                
                logger.info("Successfully decoded keys");

                // Decrypt
                String decryptedMessage = PgpUtils.decrypt(encryptedMessage, privateKey, publicKey);
                logger.info("Successfully decrypted message");
                
                return decryptedMessage;
            } catch (IllegalArgumentException e) {
                logger.error("Invalid base64 input", e);
                throw new IllegalArgumentException("Invalid encrypted message format");
            }
        } catch (Exception e) {
            logger.error("Error during decryption", e);
            throw e;
        }
    }

    private void validateInput(String email, String message) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
    }

    public boolean verifySignature(String email, String message, String signature) throws Exception {
        try {
            logger.info("Starting verification process for email: {}", email);
            
            // Get public key from database
            PgpKeyEntity keyEntity = keyManagementService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No key found for email: " + email));

            logger.info("Found key pair for email: {}", email);

            // Decode public key
            PGPPublicKey publicKey = PgpUtils.decodePublicKey(keyEntity.getPublicKey());
            logger.info("Successfully decoded public key");

            // Verify signature
            boolean isValid = PgpUtils.verifySignature(message, signature, publicKey);
            logger.info("Signature verification result: {}", isValid);
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Verification failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
