package com.learn.interviewmentor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Creates and reads JSON Web Tokens.
 *
 * A JWT is three base64 chunks separated by dots: header.payload.signature.
 * The payload is NOT encrypted - anyone can read it (paste one into jwt.io).
 * What the signature guarantees is that nobody *changed* it, because only the
 * server knows the secret key. So: never put a password or anything private in
 * the claims.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMillis) {
        // HMAC-SHA256 needs at least a 256-bit (32 byte) key.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;
    }

    /**
     * The "subject" is the email. We also stash the role and name as extra
     * claims so the frontend can render the right dashboard without an extra
     * round trip - but the server never trusts those, it re-loads the user.
     */
    public String generateToken(AppUserDetails userDetails) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claims(Map.of(
                        "role", userDetails.getUser().getRole().name(),
                        "name", userDetails.getUser().getFullName(),
                        "uid", userDetails.getUser().getId()
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the email inside the token, or null if it is invalid/expired. */
    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            // Bad signature, expired, malformed - all mean "not authenticated".
            return null;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}
