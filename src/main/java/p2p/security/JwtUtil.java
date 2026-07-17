package p2p.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Stateless utility class for generating and validating JSON Web Tokens
 * using JJWT 0.12.6 and HMAC-SHA256.
 *
 * <h2>Thread-safety</h2>
 * <p>The class holds no mutable state. The {@link SecretKey} is derived once
 * at class-load time via a {@code static final} field and is never mutated.
 * All public methods are therefore safe to call from multiple threads
 * concurrently without external synchronisation.</p>
 *
 * <h2>Token types</h2>
 * <ul>
 *   <li><b>Access token</b> – short-lived ({@value #ACCESS_TOKEN_EXPIRY_MS} ms / 15 min),
 *       carries {@code email} (subject) and {@code role} claims.</li>
 *   <li><b>Refresh token</b> – long-lived ({@value #REFRESH_TOKEN_EXPIRY_MS} ms / 7 days),
 *       carries only the {@code email} subject.</li>
 * </ul>
 *
 * <h2>Secret key</h2>
 * <p>The signing secret is read from the {@code jwt.secret} system property at
 * class-load time. If the property is absent the default development key is used.
 * <strong>Always override this with a strong, random key in production</strong>
 * by passing {@code -Djwt.secret=<base64-or-random-string>} to the JVM.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String access  = JwtUtil.generateAccessToken("alice@example.com", "USER");
 * String refresh = JwtUtil.generateRefreshToken("alice@example.com");
 *
 * boolean valid = JwtUtil.validateToken(access);
 * String  email = JwtUtil.extractEmail(access);
 * }</pre>
 */
public final class JwtUtil {

    // -----------------------------------------------------------------------
    // Configuration constants
    // -----------------------------------------------------------------------

    /** Access token lifetime: 15 minutes in milliseconds. */
    private static final long ACCESS_TOKEN_EXPIRY_MS  = 15L * 60 * 1_000;        // 15 min

    /** Refresh token lifetime: 7 days in milliseconds. */
    private static final long REFRESH_TOKEN_EXPIRY_MS = 7L * 24 * 60 * 60 * 1_000; // 7 days

    /** JWT claim key used to carry the user's authorisation role. */
    private static final String CLAIM_ROLE = "role";

    /**
     * The signing key, derived once at class-load time.
     * HMAC-SHA256 requires a minimum of 256 bits (32 bytes).
     * If the provided secret is shorter it is zero-padded to 32 bytes;
     * for security this should never be relied upon in production.
     */
    private static final SecretKey SIGNING_KEY = buildSigningKey();

    // -----------------------------------------------------------------------
    // Private constructor — utility class, not instantiable
    // -----------------------------------------------------------------------

    private JwtUtil() {
        throw new UnsupportedOperationException(
                "JwtUtil is a utility class and must not be instantiated.");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generates a signed access token for the given user.
     *
     * @param email the user's e-mail address, used as the JWT subject; must not be {@code null}
     * @param role  the user's authorisation role (e.g. {@code "USER"}, {@code "ADMIN"})
     * @return a compact, URL-safe JWT string
     * @throws IllegalArgumentException if {@code email} is {@code null} or blank
     */
    public static String generateAccessToken(String email, String role) {
        validateEmail(email);
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_MS);

        return Jwts.builder()
                   .subject(email)
                   .claim(CLAIM_ROLE, role)
                   .issuedAt(now)
                   .expiration(expiry)
                   .signWith(SIGNING_KEY)
                   .compact();
    }

    /**
     * Generates a signed refresh token for the given user.
     * Refresh tokens carry only the subject (email); role and other
     * claims must be re-fetched from the database when the token is exchanged.
     *
     * @param email the user's e-mail address; must not be {@code null} or blank
     * @return a compact, URL-safe JWT string
     * @throws IllegalArgumentException if {@code email} is {@code null} or blank
     */
    public static String generateRefreshToken(String email) {
        validateEmail(email);
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_EXPIRY_MS);

        return Jwts.builder()
                   .subject(email)
                   .issuedAt(now)
                   .expiration(expiry)
                   .signWith(SIGNING_KEY)
                   .compact();
    }

    /**
     * Extracts the subject (email) from a JWT.
     *
     * @param token a compact JWT string; must not be {@code null}
     * @return the email address embedded as the JWT subject
     * @throws JwtException             if the token is malformed, expired, or the signature is invalid
     * @throws IllegalArgumentException if {@code token} is {@code null} or blank
     */
    public static String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates a JWT: checks signature, structure, and expiry.
     *
     * @param token a compact JWT string; must not be {@code null}
     * @return {@code true} if the token is well-formed, correctly signed, and not expired;
     *         {@code false} otherwise
     */
    public static boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Treat expired tokens as invalid rather than propagating the exception
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Parses and verifies the token, returning its {@link Claims} payload.
     * Throws a {@link JwtException} sub-type on any verification failure.
     */
    private static Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(SIGNING_KEY)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    /**
     * Derives an HMAC-SHA256 {@link SecretKey} from the configured secret string.
     * The raw bytes of the secret (UTF-8) are padded to a minimum of 32 bytes
     * to satisfy the HS256 key-length requirement.
     */
    private static SecretKey buildSigningKey() {
        String secret = System.getProperty(
                "jwt.secret",
                "peerlink-super-secret-key-change-in-production");

        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);

        // HS256 requires at minimum 256 bits (32 bytes). Pad if necessary.
        if (rawBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(rawBytes, 0, padded, 0, rawBytes.length);
            rawBytes = padded;
        }

        return Keys.hmacShaKeyFor(rawBytes);
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank.");
        }
    }
}
