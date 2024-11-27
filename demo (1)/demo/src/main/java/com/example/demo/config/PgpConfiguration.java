package com.example.demo.config;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.Security;

@Configuration
public class PgpConfiguration {
    @PostConstruct
    public void addBouncyCastleProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }
}