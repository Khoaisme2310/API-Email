package com.example.demo.service;

import com.example.demo.entity.PgpKeyEntity;
import com.example.demo.repository.PgpKeyRepository;
import com.example.demo.utils.PgpUtils;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PgpKeyManagementService {

    private static final Logger log = LoggerFactory.getLogger(PgpKeyManagementService.class);

    private final PgpKeyRepository pgpKeyRepository;

    @Autowired
    public PgpKeyManagementService(PgpKeyRepository pgpKeyRepository) {
        this.pgpKeyRepository = pgpKeyRepository;
    }

    public boolean existsByEmail(String email) {
        return pgpKeyRepository.existsByEmail(email);
    }

    @Transactional
    public PgpKeyEntity generateAndSaveKeyPair(String email) throws Exception {
        try {
            if (existsByEmail(email)) {
                throw new IllegalArgumentException("Email " + email + " đã được generate key pair trước đó");
            }

            // Generate key pair
            PGPKeyPair keyPair = PgpUtils.generateKeyPair();
            
            // Encode keys
            String privateKeyStr = PgpUtils.encodePrivateKey(keyPair);
            String publicKeyStr = PgpUtils.encodePublicKey(keyPair.getPublicKey());
            
            // Create and save entity
            PgpKeyEntity entity = new PgpKeyEntity();
            entity.setEmail(email);
            entity.setPrivateKey(privateKeyStr);
            entity.setPublicKey(publicKeyStr);
            entity.setCreatedAt(LocalDateTime.now());
            
            return pgpKeyRepository.save(entity);
        } catch (Exception e) {
            log.error("Error generating key pair: ", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<PgpKeyEntity> getKeyPairByEmail(String email) {
        return pgpKeyRepository.findByEmail(email);
    }

    @Transactional
    public String decryptMessage(String email, byte[] encryptedMessage) throws Exception {
        Optional<PgpKeyEntity> keyOptional = pgpKeyRepository.findByEmail(email);
        if (keyOptional.isEmpty()) {
            throw new IllegalArgumentException("No keys found for email: " + email);
        }

        PgpKeyEntity keyEntity = keyOptional.get();
        byte[] privateKeyBytes = Base64.decode(keyEntity.getPrivateKey());

        PGPKeyPair keyPair = PgpUtils.readPrivateKey(new ByteArrayInputStream(privateKeyBytes));
        byte[] decryptedBytes = PgpUtils.decryptData(
                new ByteArrayInputStream(encryptedMessage),
                keyPair.getPrivateKey()
        );

        return new String(decryptedBytes);
    }

    @Transactional
    public boolean verifySignature(String email, byte[] originalMessage, byte[] signature) throws Exception {
        Optional<PgpKeyEntity> keyOptional = pgpKeyRepository.findByEmail(email);
        if (keyOptional.isEmpty()) {
            throw new IllegalArgumentException("No keys found for email: " + email);
        }

        PgpKeyEntity keyEntity = keyOptional.get();
        byte[] publicKeyBytes = Base64.decode(keyEntity.getPublicKey());

        return PgpUtils.verifySignature(
                originalMessage,
                signature,
                PgpUtils.readPublicKeyFromByteArray(publicKeyBytes)
        );
    }

    public List<PgpKeyEntity> findAll() {
        return pgpKeyRepository.findAll();
    }

    public Optional<PgpKeyEntity> findByEmail(String email) {
        return pgpKeyRepository.findByEmail(email);
    }
}
