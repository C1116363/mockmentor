package com.learn.interviewmentor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationMillis;

    /**
     * The development default, spelled out so it can be recognised.
     *
     * It is committed to a public repository, which means anyone who can read
     * the repo can sign a token for any account on any server still using it -
     * including an admin one. That is a total authentication bypass, and it
     * looks like nothing is wrong: the app works perfectly.
     */
    private static final String DEV_SECRET =
            "c3ByaW5nLWJvb3QtbGVhcm5pbmctcHJvamVjdC1qd3Qtc2lnbmluZy1rZXktMzJiKw==";

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMillis) {
        // HMAC-SHA256 needs at least a 256-bit (32 byte) key.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMillis = expirationMillis;

        if (DEV_SECRET.equals(base64Secret.trim())) {
            // Warn rather than refuse. Refusing would stop a fresh clone from
            // starting at all, and the whole point of the default is that the
            // project runs with no configuration. The banner is deliberately
            // impossible to scroll past.
            log.warn("""

                    ================================================================
                     JWT_SECRET IS THE PUBLIC DEVELOPMENT DEFAULT
                    ================================================================
                     This value is committed to the repository. Anyone who can
                     read it can forge a token for ANY account, including admin.
                     Nothing will look broken - that is what makes it dangerous.

                     Before this is reachable from the internet, set your own:
                       openssl rand -base64 32
                     then put it in the environment as JWT_SECRET.
                    ================================================================""");
        }
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

    /**
     * The verified claims, or null if the token is invalid, expired or forged.
     *
     * Returned whole rather than one field at a time so a caller that needs two
     * of them - the filter needs the subject and the issued-at - parses and
     * verifies the signature once instead of twice.
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            // Bad signature, expired, malformed - all mean "not authenticated".
            return null;
        }
    }

    /** Returns the email inside the token, or null if it is invalid/expired. */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    /**
     * When the token was issued, as local time, or null if unreadable.
     *
     * Used to refuse tokens older than the account's last password change.
     * "iat" is seconds since the epoch in UTC, so it has to come back through
     * the system zone to be comparable with the LocalDateTime columns the rest
     * of this app stores.
     */
    public LocalDateTime extractIssuedAt(String token) {
        Claims claims = parseClaims(token);
        if (claims == null || claims.getIssuedAt() == null) {
            return null;
        }
        return LocalDateTime.ofInstant(claims.getIssuedAt().toInstant(), ZoneId.systemDefault());
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}
