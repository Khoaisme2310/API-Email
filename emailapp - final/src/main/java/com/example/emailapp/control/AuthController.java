package com.example.emailapp.control;

import com.example.emailapp.model.User;
import com.example.emailapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        logger.info("Registering user with email: {}", user.getEmail());
        return ResponseEntity.ok(userService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        try {
            String base64Credentials = authorizationHeader.split("Basic ")[1];
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] values = credentials.split(":", 2);
            if (values.length != 2) {
                return ResponseEntity.badRequest().body("Invalid Basic Auth format");
            }

            String email = values[0];
            String password = values[1];

            logger.info("Attempting login for email: {}", email);

            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (password.equals(user.getPassword())) {
                logger.info("Login successful for email: {}", email);
                return ResponseEntity.ok(user);
            } else {
                logger.error("Invalid credentials for email: {}", email);
                return ResponseEntity.status(401).body("Invalid credentials");
            }
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(400).body("Bad Request");
        }
    }
}
