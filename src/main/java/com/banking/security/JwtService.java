package com.banking.security;

import com.banking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenSeconds;
    private final String issuer;

    public JwtService(@Value("${banking.jwt.secret}") String secret,
                      @Value("${banking.jwt.access-token-minutes:30}") long accessTokenMinutes,
                      @Value("${banking.jwt.issuer:banking-management-system}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenSeconds = accessTokenMinutes * 60;
        this.issuer = issuer;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).toList();
        return Jwts.builder()
                .subject(user.getEmail())
                .issuer(issuer)
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token, SecurityUser user) {
        try {
            Claims claims = parse(token);
            return claims.getSubject().equalsIgnoreCase(user.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }
}
