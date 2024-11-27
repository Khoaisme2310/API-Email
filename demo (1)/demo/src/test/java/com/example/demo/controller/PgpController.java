package com.example.demo.controller;

import com.example.demo.service.PgpService;
import com.example.demo.service.PgpKeyManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.example.demo.entity.PgpKeyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/pgp")
public class PgpController {
    private final PgpService pgpService;
    private final PgpKeyManagementService keyManagementService;
    private static final Logger logger = LoggerFactory.getLogger(PgpController.class);

    @Autowired
    public PgpController(PgpService pgpService, PgpKeyManagementService keyManagementService) {
        this.pgpService = pgpService;
        this.keyManagementService = keyManagementService;
    }

    @PostMapping("/generate-key")
    public ResponseEntity<String> generateKeyPair(@RequestParam String email) {
        try {
            if (keyManagementService.existsByEmail(email)) {
                return ResponseEntity.badRequest()
                    .body("Email " + email + " đã được generate key pair trước đó");
            }

            PgpKeyEntity keyEntity = keyManagementService.generateAndSaveKeyPair(email);
            return ResponseEntity.ok("Key pair generated successfully for " + email);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input for key generation", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Key generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Key generation failed: " + e.getMessage());
        }
    }

    @PostMapping("/encrypt")
    public ResponseEntity<String> encrypt(
            @RequestParam String email,
            @RequestBody String message
    ) {
        try {
            String encrypted = pgpService.encryptMessage(email, message);
            return ResponseEntity.ok(encrypted);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Encryption failed");
        }
    }

    @PostMapping("/sign")
    public ResponseEntity<String> sign(
            @RequestParam String email,
            @RequestBody String message
    ) {
        try {
            logger.info("Signing request received for email: {}", email);
            logger.info("Message length: {}", message.length());

            String signature = pgpService.signMessage(email, message);
            return ResponseEntity.ok(signature);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input for signing", e);
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            logger.error("Signing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Signing failed: " + e.getMessage());
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<String> decrypt(
            @RequestParam String email,
            @RequestBody String encryptedMessage
    ) {
        try {
            String decryptedMessage = pgpService.decryptMessage(email, encryptedMessage);
            return ResponseEntity.ok(decryptedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Decryption failed: " + e.getMessage());
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<String> checkEmail(@RequestParam String email) {
        try {
            boolean exists = keyManagementService.existsByEmail(email);
            if (exists) {
                return ResponseEntity.ok("Email " + email + " đã được generate key pair");
            } else {
                return ResponseEntity.ok("Email " + email + " chưa được generate key pair");
            }
        } catch (Exception e) {
            logger.error("Error checking email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking email: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(
            @RequestParam String email,
            @RequestParam String signature,
            @RequestBody String message
    ) {
        try {
            logger.info("Verifying signature for email: {}", email);
            logger.info("Message length: {}", message.length());
            logger.info("Signature length: {}", signature.length());

            boolean isValid = pgpService.verifySignature(email, message, signature);
            
            if (isValid) {
                return ResponseEntity.ok("Chữ ký hợp lệ - Message đến từ " + email + " và không bị sửa đổi");
            } else {
                return ResponseEntity.ok("Chữ ký không hợp lệ!");
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input for verification", e);
            return ResponseEntity.badRequest().body(e.getMessage());
            
        } catch (Exception e) {
            logger.error("Verification failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Verification failed: " + e.getMessage());
        }
    }
}