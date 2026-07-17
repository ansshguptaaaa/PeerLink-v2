package p2p.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Stateless utility class for BCrypt password hashing and verification.
 *
 * <h2>Thread-safety</h2>
 * <p>The class holds no mutable state. Both methods delegate exclusively to
 * {@link BCrypt}, whose static methods are themselves thread-safe. Any number
 * of threads may call {@link #hashPassword} and {@link #verifyPassword}
 * concurrently without external synchronisation.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Hash a plaintext password before storing it:
 * String hash = PasswordUtil.hashPassword("mySecret123");
 *
 * // Verify at login time:
 * boolean ok = PasswordUtil.verifyPassword("mySecret123", hash);
 * }</pre>
 *
 * <h2>Security notes</h2>
 * <ul>
 *   <li>Each call to {@link #hashPassword} generates a fresh, random salt via
 *       {@link BCrypt#gensalt(int)}, so two hashes of the same password are
 *       intentionally different — never compare hashes with {@code equals()}.</li>
 *   <li>The work factor (log-rounds) is {@value #BCRYPT_ROUNDS}. Increase it
 *       as hardware speeds up; the {@link #verifyPassword} method is
 *       self-calibrating because the round count is embedded in the hash string.</li>
 *   <li>This class never logs, stores, or returns the plaintext password.</li>
 * </ul>
 */
public final class PasswordUtil {

    /**
     * BCrypt cost factor (log₂ of the number of iterations).
     * 12 rounds ≈ 250 ms on modern hardware — a good balance between
     * user experience and brute-force resistance.
     * Accepted range: 4–31.
     */
    private static final int BCRYPT_ROUNDS = 12;

    // -----------------------------------------------------------------------
    // Private constructor — prevents instantiation and subclassing
    // -----------------------------------------------------------------------

    private PasswordUtil() {
        throw new UnsupportedOperationException(
                "PasswordUtil is a utility class and must not be instantiated.");
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Hashes a plaintext password using BCrypt with a randomly generated salt.
     *
     * <p>The returned string contains the algorithm identifier, cost factor,
     * salt, and digest — everything {@link #verifyPassword} needs to verify the
     * password later. Store this string as-is in the database.</p>
     *
     * @param password the plaintext password to hash; must not be {@code null} or blank
     * @return a 60-character BCrypt hash string
     * @throws IllegalArgumentException if {@code password} is {@code null} or blank
     */
    public static String hashPassword(String password) {
        validatePassword(password);
        String salt = BCrypt.gensalt(BCRYPT_ROUNDS);
        return BCrypt.hashpw(password, salt);
    }

    /**
     * Verifies that {@code rawPassword} matches the previously hashed value.
     *
     * <p>Comparison is performed in constant time to mitigate timing-based
     * side-channel attacks (this is handled internally by {@link BCrypt#checkpw}).</p>
     *
     * @param rawPassword    the plaintext password supplied by the user at login;
     *                       must not be {@code null} or blank
     * @param hashedPassword the BCrypt hash retrieved from persistent storage;
     *                       must not be {@code null} or blank
     * @return {@code true} if the plaintext matches the hash, {@code false} otherwise
     * @throws IllegalArgumentException if either argument is {@code null} or blank
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        validatePassword(rawPassword);
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("hashedPassword must not be null or blank.");
        }
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or blank.");
        }
    }
}
