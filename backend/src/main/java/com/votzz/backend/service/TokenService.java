package com.votzz.backend.service;

import com.votzz.backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class TokenService {

    // Lê do application.properties. Se não existir, a aplicação nem sobe (segurança).
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration; // Ex: 86400000 para 24h

   // --- GERAÇÃO DO TOKEN ---
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        
        if (user.getTenant() != null) {
            claims.put("tenantId", user.getTenant().getId());
        }

        // Usamos o Email como Subject (identificador principal)
        return createToken(claims, user.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // --- VALIDAÇÃO (Adaptado para o SecurityFilter) ---
    
    // Este método é chamado pelo SecurityFilter. 
    // Ele tenta ler o token. Se conseguir, retorna o Email (Subject). Se der erro (expirado/inválido), retorna vazio.
    public String validateToken(String token) {
        try {
            return extractUsername(token);
        } catch (Exception e) {
            // Se o token estiver expirado ou assinatura inválida, o parser lança exceção.
            // Retornamos vazio para indicar que a validação falhou.
            return "";
        }
    }

    // --- MÉTODOS AUXILIARES ---

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}