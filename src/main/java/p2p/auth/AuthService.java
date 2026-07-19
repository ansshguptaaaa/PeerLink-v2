package p2p.auth;

import p2p.db.ConnectionManager;
import p2p.model.User;
import p2p.repository.UserRepository;
import p2p.repository.EmailVerificationRepository;
import p2p.security.JwtUtil;
import p2p.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Application-level authentication service for PeerLink.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>User registration ({@link #signup}) with BCrypt password hashing</li>
 *   <li>Credential validation ({@link #login}) with JWT issuance</li>
 *   <li>Access-token renewal ({@link #refresh}) via a stored refresh token</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>The only instance field is {@link UserRepository}, which is itself
 * stateless. {@link PasswordUtil} and {@link JwtUtil} are stateless utility
 * classes. Refresh-token DB operations borrow connections from the HikariCP
 * pool and release them immediately via try-with-resources. Consequently
 * multiple threads may call any method concurrently without external
 * synchronisation.</p>
 *
 * <h2>Required DDL</h2>
 * <pre>{@code
 * -- Run once before starting the application
 * CREATE TABLE IF NOT EXISTS users (
 *     id          BIGSERIAL    PRIMARY KEY,
 *     username    VARCHAR(50)  NOT NULL UNIQUE,
 *     email       VARCHAR(255) NOT NULL UNIQUE,
 *     password    VARCHAR(255) NOT NULL,
 *     role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
 *     is_verified BOOLEAN      NOT NULL DEFAULT FALSE,
 *     created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
 *     last_login  TIMESTAMP
 * );
 *
 * CREATE TABLE IF NOT EXISTS refresh_tokens (
 *     id         BIGSERIAL PRIMARY KEY,
 *     user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 *     token      TEXT      NOT NULL UNIQUE,
 *     expires_at TIMESTAMP NOT NULL,
 *     created_at TIMESTAMP NOT NULL DEFAULT NOW()
 * );
 * CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AuthService auth = new AuthService();
 *
 * AuthResponse r1 = auth.signup("alice", "alice@example.com", "secret123");
 * AuthResponse r2 = auth.login("alice@example.com", "secret123");
 * AuthResponse r3 = auth.refresh(r2.refreshToken());
 * }</pre>
 */
public class AuthService {

    // -----------------------------------------------------------------------
    // Inner types – AuthResponse and AuthException
    // -----------------------------------------------------------------------

    /**
     * Immutable value object returned by every successful auth operation.
     * Both tokens are compact, URL-safe JWT strings.
     */
    public static final class AuthResponse {

        private final String accessToken;
        private final String refreshToken;
        private final String email;
        private final String role;

        AuthResponse(String accessToken, String refreshToken, String email, String role) {
            this.accessToken  = accessToken;
            this.refreshToken = refreshToken;
            this.email        = email;
            this.role         = role;
        }

        /** Short-lived JWT (15 minutes). Send in {@code Authorization: Bearer <token>} header. */
        public String getAccessToken()  { return accessToken;  }

        /** Long-lived JWT (7 days). Store securely and use to obtain new access tokens. */
        public String getRefreshToken() { return refreshToken; }

        /** The authenticated user's e-mail address. */
        public String getEmail()        { return email;        }

        /** The authenticated user's role, e.g. {@code "USER"} or {@code "ADMIN"}. */
        public String getRole()         { return role;         }

        @Override
        public String toString() {
            return "AuthResponse{email='" + email + "', role='" + role + "'}";
        }
    }

    /**
     * Unchecked exception thrown for all authentication and authorisation
     * failures. The {@link ErrorCode} enum distinguishes failure categories
     * so callers can map them to appropriate HTTP status codes without
     * parsing the message string.
     */
    public static final class AuthException extends RuntimeException {

        /** Classifies the failure reason. */
        public enum ErrorCode {
            /** The provided e-mail address is already registered. */
            EMAIL_ALREADY_EXISTS,
            /** No account exists for the given e-mail. */
            USER_NOT_FOUND,
            /** The supplied plaintext password does not match the stored hash. */
            INVALID_CREDENTIALS,
            /** The refresh token is missing, expired, revoked, or structurally invalid. */
            INVALID_REFRESH_TOKEN,
            /** A database or I/O error occurred during the operation. */
            DATABASE_ERROR,
            INVALID_OTP,
            OTP_EXPIRED,
            TOO_MANY_OTP_REQUESTS,
        }

        private final ErrorCode errorCode;

        AuthException(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        AuthException(ErrorCode errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        /** Returns the structured failure reason. */
        public ErrorCode getErrorCode() { return errorCode; }
    }

    // -----------------------------------------------------------------------
    // SQL constants – refresh_tokens table
    // -----------------------------------------------------------------------

    /** Refresh token lifetime mirrors JwtUtil: 7 days. */
    private static final long REFRESH_TOKEN_TTL_MS = 7L * 24 * 60 * 60 * 1_000;

    private static final String SQL_INSERT_REFRESH_TOKEN =
            "INSERT INTO refresh_tokens (user_id, token, expiry_date) VALUES (?, ?, ?)";

    private static final String SQL_FIND_REFRESH_TOKEN =
            "SELECT user_id, expiry_date FROM refresh_tokens WHERE token = ?";

    private static final String SQL_DELETE_REFRESH_TOKEN =
            "DELETE FROM refresh_tokens WHERE token = ?";

    private static final String SQL_DELETE_EXPIRED_TOKENS =
            "DELETE FROM refresh_tokens WHERE user_id = ? AND expiry_date < NOW()";

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates an {@code AuthService} with default (production) dependencies.
     */
     public AuthService() {
         this(new UserRepository(), new EmailVerificationRepository());
     }
 
     /**
      * Creates an {@code AuthService} with an explicit {@link UserRepository}.
      */
     public AuthService(UserRepository userRepository) {
         this(userRepository, new EmailVerificationRepository());
     }
 
     public AuthService(UserRepository userRepository, EmailVerificationRepository emailVerificationRepository) {
         if (userRepository == null) {
             throw new IllegalArgumentException("userRepository must not be null.");
         }
         if (emailVerificationRepository == null) {
             throw new IllegalArgumentException("emailVerificationRepository must not be null.");
         }
         this.userRepository = userRepository;
         this.emailVerificationRepository = emailVerificationRepository;
     }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * <ol>
     *   <li>Validates that the e-mail is not already taken.</li>
     *   <li>Hashes the plaintext password with BCrypt (12 rounds).</li>
     *   <li>Persists the new {@link User} row.</li>
     *   <li>Issues access + refresh tokens and stores the refresh token.</li>
     * </ol>
     *
     * @param username non-null, non-blank display name
     * @param email    non-null, non-blank unique e-mail address
     * @param password non-null, non-blank plaintext password (will be hashed)
     * @return {@link AuthResponse} containing both tokens and user metadata
     * @throws AuthException            with {@link AuthException.ErrorCode#EMAIL_ALREADY_EXISTS}
     *                                  if the e-mail is already registered
     * @throws AuthException            with {@link AuthException.ErrorCode#DATABASE_ERROR}
     *                                  on any persistence failure
     * @throws IllegalArgumentException if any argument is {@code null} or blank
     */
    public AuthResponse signup(String username, String email, String password) {
        validateNotBlank(username, "username");
        validateNotBlank(email,    "email");
        validateNotBlank(password, "password");

        try {

            // 1. Guard duplicate e-mail
            if (userRepository.existsByEmail(email)) {
                throw new AuthException(
                        AuthException.ErrorCode.EMAIL_ALREADY_EXISTS,
                        "An account with email '" + email + "' already exists.");
            }

            // 2. Hash password
            String hashedPassword = PasswordUtil.hashPassword(password);

            // 3. Persist user (id and createdAt are populated by save())
            User user = new User(username, email, hashedPassword, "USER");
            userRepository.save(user);

            // 4. Issue tokens
            String accessToken  = JwtUtil.generateAccessToken(email, user.getRole());
            String refreshToken = JwtUtil.generateRefreshToken(email);

            // 5. Persist refresh token
            storeRefreshToken(user.getId(), refreshToken);

            // Clean up verification record after successful signup

            return new AuthResponse(accessToken, refreshToken, email, user.getRole());

        } catch (AuthException e) {
            throw e; // re-throw auth-domain exceptions unchanged
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "signup: database error – " + e.getMessage(), e);
        }
    }

    public void sendOtp(String email) {
        validateNotBlank(email, "email");

        try {
            // Guard duplicate email in users table
            if (userRepository.existsByEmail(email)) {
                throw new AuthException(
                        AuthException.ErrorCode.EMAIL_ALREADY_EXISTS,
                        "An account with email '" + email + "' already exists.");
            }

            // Check if there was a recent request (Too many OTP requests: 30 seconds limit)
            EmailVerificationRepository.VerificationRecord record = emailVerificationRepository.getVerification(email);
            if (record != null) {
                long diffMs = Math.max(0, Instant.now().toEpochMilli() - record.getCreatedAt().toInstant().toEpochMilli());
                if (diffMs < 30 * 1000) {
                    throw new AuthException(
                            AuthException.ErrorCode.TOO_MANY_OTP_REQUESTS,
                            "Please wait " + (30 - (diffMs / 1000)) + " seconds before requesting another code.");
                }
            }

            // Generate random 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
            Timestamp expiresAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            // Save to database
            emailVerificationRepository.saveOrUpdateOtp(email, otp, expiresAt);

            // Send via email
            p2p.service.EmailService.sendOtpEmail(email, otp);

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "sendOtp: database error – " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "sendOtp: error – " + e.getMessage(), e);
        }
    }

    public void verifyOtp(String email, String otp) {
        validateNotBlank(email, "email");
        validateNotBlank(otp,   "otp");

        try {
            EmailVerificationRepository.VerificationRecord record = emailVerificationRepository.getVerification(email);
            if (record == null || !otp.equals(record.getOtp())) {
                throw new AuthException(
                        AuthException.ErrorCode.INVALID_OTP,
                        "Invalid verification code. Please check and try again.");
            }

            if (Instant.now().isAfter(record.getExpiresAt().toInstant())) {
                throw new AuthException(
                        AuthException.ErrorCode.OTP_EXPIRED,
                        "Verification code has expired. Please request a new one.");
            }

            // Mark as verified
            emailVerificationRepository.markAsVerified(email);

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "verifyOtp: database error – " + e.getMessage(), e);
        }
    }

    public void forgotPassword(String email) {
        validateNotBlank(email, "email");

        try {
            // Check if account exists.
            if (!userRepository.existsByEmail(email)) {
                // Return silently to not reveal email existence
                return;
            }

            // Check if there was a recent request (Too many OTP requests: 30 seconds limit)
            EmailVerificationRepository.VerificationRecord record = emailVerificationRepository.getVerification(email);
            if (record != null) {
                long diffMs = Math.max(0, Instant.now().toEpochMilli() - record.getCreatedAt().toInstant().toEpochMilli());
                if (diffMs < 30 * 1000) {
                    throw new AuthException(
                            AuthException.ErrorCode.TOO_MANY_OTP_REQUESTS,
                            "Please wait " + (30 - (diffMs / 1000)) + " seconds before requesting another code.");
                }
            }

            // Generate random 6-digit OTP
            String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
            Timestamp expiresAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            // Save to database
            emailVerificationRepository.saveOrUpdateOtp(email, otp, expiresAt);

            // Send via email
            p2p.service.EmailService.sendOtpEmail(email, otp);

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "forgotPassword: database error – " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "forgotPassword: error – " + e.getMessage(), e);
        }
    }

    public void verifyResetOtp(String email, String otp) {
        validateNotBlank(email, "email");
        validateNotBlank(otp,   "otp");

        try {
            EmailVerificationRepository.VerificationRecord record = emailVerificationRepository.getVerification(email);
            if (record == null || !otp.equals(record.getOtp())) {
                throw new AuthException(
                        AuthException.ErrorCode.INVALID_OTP,
                        "Invalid verification code. Please check and try again.");
            }

            if (Instant.now().isAfter(record.getExpiresAt().toInstant())) {
                throw new AuthException(
                        AuthException.ErrorCode.OTP_EXPIRED,
                        "Verification code has expired. Please request a new one.");
            }
        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "verifyResetOtp: database error – " + e.getMessage(), e);
        }
    }

    public void resetPassword(String email, String otp, String newPassword) {
        validateNotBlank(email,       "email");
        validateNotBlank(otp,         "otp");
        validateNotBlank(newPassword, "newPassword");

        try {
            // 1. Verify OTP again
            EmailVerificationRepository.VerificationRecord record = emailVerificationRepository.getVerification(email);
            if (record == null || !otp.equals(record.getOtp())) {
                throw new AuthException(
                        AuthException.ErrorCode.INVALID_OTP,
                        "Invalid verification code. Please check and try again.");
            }

            if (Instant.now().isAfter(record.getExpiresAt().toInstant())) {
                throw new AuthException(
                        AuthException.ErrorCode.OTP_EXPIRED,
                        "Verification code has expired. Please request a new one.");
            }

            // 2. Hash password using existing PasswordUtil
            String hashedPassword = PasswordUtil.hashPassword(newPassword);

            // 3. Update users table password
            userRepository.updatePassword(email, hashedPassword);

            // 4. Delete used OTP after successful reset

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "resetPassword: database error – " + e.getMessage(), e);
        }
    }

    /**
     * Authenticates an existing user by e-mail and password.
     *
     * <ol>
     *   <li>Looks up the user by e-mail.</li>
     *   <li>Verifies the plaintext password against the stored BCrypt hash.</li>
     *   <li>Updates {@code last_login} and removes stale refresh tokens for the user.</li>
     *   <li>Issues fresh access + refresh tokens and stores the new refresh token.</li>
     * </ol>
     *
     * @param email    the user's registered e-mail address
     * @param password the plaintext password to verify
     * @return {@link AuthResponse} containing both tokens and user metadata
     * @throws AuthException            with {@link AuthException.ErrorCode#USER_NOT_FOUND}
     *                                  if no account exists for the given e-mail
     * @throws AuthException            with {@link AuthException.ErrorCode#INVALID_CREDENTIALS}
     *                                  if the password does not match
     * @throws AuthException            with {@link AuthException.ErrorCode#DATABASE_ERROR}
     *                                  on any persistence failure
     * @throws IllegalArgumentException if any argument is {@code null} or blank
     */
    public AuthResponse login(String email, String password) {
        validateNotBlank(email,    "email");
        validateNotBlank(password, "password");

        try {
            // 1. Look up user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AuthException(
                            AuthException.ErrorCode.USER_NOT_FOUND,
                            "No account found for email: " + email));

            // 2. Verify password
            if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
                throw new AuthException(
                        AuthException.ErrorCode.INVALID_CREDENTIALS,
                        "Invalid password for email: " + email);
            }

            // 3. Update last login & prune expired tokens (best-effort, non-fatal)
            try {
                userRepository.updateLastLogin(user.getId());
                deleteExpiredTokensForUser(user.getId());
            } catch (SQLException e) {
                // Log and continue — login should not fail on a housekeeping error
                System.err.println("[AuthService] Warning: post-login housekeeping failed – "
                        + e.getMessage());
            }

            // 4. Issue tokens
            String accessToken  = JwtUtil.generateAccessToken(email, user.getRole());
            String refreshToken = JwtUtil.generateRefreshToken(email);

            // 5. Persist refresh token
            storeRefreshToken(user.getId(), refreshToken);

            return new AuthResponse(accessToken, refreshToken, email, user.getRole());

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "login: database error – " + e.getMessage(), e);
        }
    }

    /**
     * Exchanges a valid refresh token for a new access token (and a rotated refresh token).
     *
     * <p>Token rotation: the supplied refresh token is deleted from the database and a brand-new
     * refresh token is issued and stored. This limits the damage window if a refresh token is
     * ever compromised.</p>
     *
     * @param refreshToken the refresh token previously issued by {@link #login} or {@link #signup}
     * @return {@link AuthResponse} containing a fresh access token, a new refresh token,
     *         and the user's e-mail and role
     * @throws AuthException            with {@link AuthException.ErrorCode#INVALID_REFRESH_TOKEN}
     *                                  if the token is absent, expired, revoked, or malformed
     * @throws AuthException            with {@link AuthException.ErrorCode#USER_NOT_FOUND}
     *                                  if the associated user no longer exists
     * @throws AuthException            with {@link AuthException.ErrorCode#DATABASE_ERROR}
     *                                  on any persistence failure
     * @throws IllegalArgumentException if {@code refreshToken} is {@code null} or blank
     */
    public AuthResponse refresh(String refreshToken) {
        validateNotBlank(refreshToken, "refreshToken");

        // 1. Validate JWT signature + expiry first (cheap, no DB hit)
        if (!JwtUtil.validateToken(refreshToken)) {
            throw new AuthException(
                    AuthException.ErrorCode.INVALID_REFRESH_TOKEN,
                    "Refresh token is expired or structurally invalid.");
        }

        try {
            // 2. Verify the token exists in the database (not revoked)
            Long userId = findRefreshTokenUserId(refreshToken);
            if (userId == null) {
                throw new AuthException(
                        AuthException.ErrorCode.INVALID_REFRESH_TOKEN,
                        "Refresh token has been revoked or does not exist.");
            }

            // 3. Extract the email embedded in the token
            String email = JwtUtil.extractEmail(refreshToken);

            // 4. Look up user to get the current role
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AuthException(
                            AuthException.ErrorCode.USER_NOT_FOUND,
                            "User account associated with refresh token no longer exists."));

            // 5. Rotate: delete old token, issue and store a new one
            deleteRefreshToken(refreshToken);
            String newAccessToken  = JwtUtil.generateAccessToken(email, user.getRole());
            String newRefreshToken = JwtUtil.generateRefreshToken(email);
            storeRefreshToken(userId, newRefreshToken);

            return new AuthResponse(newAccessToken, newRefreshToken, email, user.getRole());

        } catch (AuthException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthException(
                    AuthException.ErrorCode.DATABASE_ERROR,
                    "refresh: database error – " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers – refresh_tokens table operations
    // -----------------------------------------------------------------------

    /**
     * Inserts a new refresh-token row.
     * Expired tokens for the same user are pruned before inserting to keep the table lean.
     */
    private void storeRefreshToken(Long userId, String token) throws SQLException {
        Timestamp expiresAt = Timestamp.from(
                Instant.now().plus(REFRESH_TOKEN_TTL_MS, ChronoUnit.MILLIS));

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_REFRESH_TOKEN)) {

            ps.setLong(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        }
    }

    /**
     * Looks up the {@code user_id} associated with a stored (non-expired) refresh token.
     *
     * @return the {@code user_id} if the token exists and has not expired, otherwise {@code null}
     */
    private Long findRefreshTokenUserId(String token) throws SQLException {
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_REFRESH_TOKEN)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expiry_date");
                    // Guard against a DB clock drift edge case
                    if (expiresAt != null && expiresAt.toInstant().isBefore(Instant.now())) {
                        return null; // treat as expired
                    }
                    return rs.getLong("user_id");
                }
            }
        }
        return null;
    }

    /**
     * Hard-deletes a single refresh token (used during rotation and logout).
     */
    private void deleteRefreshToken(String token) throws SQLException {
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_REFRESH_TOKEN)) {

            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    /**
     * Removes all expired refresh tokens for a given user.
     * Called as housekeeping after a successful login — failure is non-fatal.
     */
    private void deleteExpiredTokensForUser(Long userId) throws SQLException {
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_EXPIRED_TOKENS)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Private utility
    // -----------------------------------------------------------------------

    private static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank.");
        }
    }
}


