package com.votzz.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class KiwifySecurityService {

    @Value("${votzz.kiwify.secret}")
    private String secretKey;

    /**
     * Validação Anti-Hacker: Verifica se o payload realmente veio da Kiwify
     */
    public boolean isSignatureValid(String signature, String payload) {
        try {
            Mac sha1Hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            sha1Hmac.init(secretKeySpec);
            
            byte[] hashBytes = sha1Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Erro ao validar assinatura Kiwify", e);
            return false;
        }
    }
}