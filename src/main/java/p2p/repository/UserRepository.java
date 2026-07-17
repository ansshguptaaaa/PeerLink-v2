package p2p.repository;

import p2p.db.ConnectionManager;
import p2p.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * JDBC repository for {@link User} persistence backed by PostgreSQL.
 *
 * <h2>Thread-safety</h2>
 * <p>This class holds <em>no mutable state</em>. Every method borrows a connection
 * from the {@link ConnectionManager} singleton (HikariCP pool) for the duration of
 * a single operation and returns it immediately via try-with-resources. Multiple
 * threads may call any method concurrently without external synchronisation.</p>
 *
 * <h2>Expected DDL</h2>
 * <pre>{@code
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
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * UserRepository repo = new UserRepository();
 *
 * User u = new User("alice", "alice@example.com", hash, "USER");
 * repo.save(u);                                   // u.getId() is populated
 *
 * Optional<User> found = repo.findByEmail("alice@example.com");
 * boolean exists       = repo.existsByEmail("alice@example.com");
 * repo.updateLastLogin(u.getId());
 * }</pre>
 */
public class UserRepository {

    // -----------------------------------------------------------------------
    // SQL constants
    // -----------------------------------------------------------------------

    private static final String SQL_INSERT =
            "INSERT INTO users (username, email, password, role, is_verified, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?) " +
            "RETURNING id, created_at";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT id, username, email, password, role, is_verified, created_at, last_login " +
            "FROM users WHERE email = ?";

    private static final String SQL_EXISTS_BY_EMAIL =
            "SELECT 1 FROM users WHERE email = ? LIMIT 1";

    private static final String SQL_UPDATE_LAST_LOGIN =
            "UPDATE users SET last_login = ? WHERE id = ?";

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates a new {@code UserRepository}.
     * The underlying {@link ConnectionManager} is accessed as a singleton;
     * no connection is opened at construction time.
     */
    public UserRepository() {
        // intentionally empty — stateless
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Persists a new user to the database.
     *
     * <p>On success the generated {@code id} and the database-assigned
     * {@code createdAt} timestamp are written back into {@code user} via
     * the {@code RETURNING} clause, so the passed object is fully populated
     * after the call returns.</p>
     *
     * @param user the user to insert; {@code id} and {@code createdAt} may be {@code null}
     *             — they are populated by the database
     * @throws IllegalArgumentException if {@code user} is {@code null}
     * @throws SQLException             if the insert fails (e.g. unique-constraint violation)
     */
    public void save(User user) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null.");
        }

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            Timestamp now = Timestamp.from(Instant.now());

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole() != null ? user.getRole() : "USER");
            ps.setBoolean(5, user.getIsVerified() != null && user.getIsVerified());
            ps.setTimestamp(6, now);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Write the DB-generated values back into the caller's object
                    user.setId(rs.getLong("id"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                }
            }
        }
    }

    /**
     * Looks up a user by their e-mail address.
     *
     * @param email the e-mail to search for; must not be {@code null} or blank
     * @return an {@link Optional} containing the matching {@link User},
     *         or {@link Optional#empty()} if no record exists
     * @throws IllegalArgumentException if {@code email} is {@code null} or blank
     * @throws SQLException             if the query fails
     */
    public Optional<User> findByEmail(String email) throws SQLException {
        validateEmail(email);

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Checks whether a user with the given e-mail address already exists.
     *
     * <p>Uses a {@code SELECT 1 … LIMIT 1} projection for maximum efficiency —
     * no row data is transferred beyond a single byte.</p>
     *
     * @param email the e-mail to check; must not be {@code null} or blank
     * @return {@code true} if at least one row matches, {@code false} otherwise
     * @throws IllegalArgumentException if {@code email} is {@code null} or blank
     * @throws SQLException             if the query fails
     */
    public boolean existsByEmail(String email) throws SQLException {
        validateEmail(email);

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_EMAIL)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Records the current UTC time as the {@code last_login} timestamp for the given user.
     *
     * <p>Intended to be called immediately after a successful authentication check.</p>
     *
     * @param userId the surrogate primary key of the user; must not be {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @throws SQLException             if the update fails or no row is matched
     */
    public void updateLastLogin(Long userId) throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null.");
        }

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {

            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setLong(2, userId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException(
                        "updateLastLogin: no user found with id=" + userId);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Maps the current row of a {@link ResultSet} to a fully-populated {@link User}.
     * The cursor must already be positioned on a valid row before calling this method.
     */
    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getBoolean("is_verified"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("last_login")   // returns null if DB value is NULL
        );
    }

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE users SET password = ? WHERE email = ?";

    public void updatePassword(String email, String hashedPassword) throws SQLException {
        validateEmail(email);
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("hashedPassword must not be null or blank.");
        }

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {

            ps.setString(1, hashedPassword);
            ps.setString(2, email);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("updatePassword: no user found with email=" + email);
            }
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be null or blank.");
        }
    }
}
