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

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long standardExpiration; // Tempo padrão (ex: 24h ou 2h) definido no properties

    // 30 dias em milissegundos: 30 * 24 * 60 * 60 * 1000
    private static final long LONG_EXPIRATION = 2592000000L;

    // Método original (mantém compatibilidade com chamadas antigas)
    public String generateToken(User user) {
        return generateToken(user, false);
    }

    // NOVO MÉTODO: Aceita a flag keepLogged para decidir a duração
    public String generateToken(User user, boolean keepLogged) {
        Map<String, Object> claims = new HashMap<>();
        // Garante que a role seja salva como String
        claims.put("role", user.getRole().name());

        if (user.getTenant() != null) {
            claims.put("tenantId", user.getTenant().getId());
        }

        // Se keepLogged for true, usa 30 dias. Senão, usa o padrão do properties.
        long validity = keepLogged ? LONG_EXPIRATION : standardExpiration;

        return createToken(claims, user.getEmail(), validity);
    }

    // Atualizado para receber a validade específica
    private String createToken(Map<String, Object> claims, String subject, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity)) // Usa a validade passada
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String validateToken(String token) {
        try {
            return extractUsername(token);
        } catch (Exception e) {
            return ""; // Token inválido
        }
    }

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