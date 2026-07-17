package p2p.dto;

/**
 * Data Transfer Object representing the authentication response returned to the client
 * after a successful login, signup, or token-refresh operation.
 *
 * <p>This is a standalone DTO in the {@code p2p.dto} package, distinct from the
 * {@code AuthService.AuthResponse} inner class used internally by the service layer.
 * The HTTP layer maps the service result to this DTO before serialising it to JSON.</p>
 */
public class AuthResponse {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /**
     * Short-lived JWT access token (valid for 15 minutes).
     * Clients should send this in the {@code Authorization: Bearer <token>} header.
     */
    private String accessToken;

    /**
     * Long-lived JWT refresh token (valid for 7 days).
     * Store securely (e.g. HTTP-only cookie) and use to obtain a new access token
     * when the current one expires.
     */
    private String refreshToken;

    /** The authenticated user's e-mail address. */
    private String email;

    /** The authenticated user's authorisation role, e.g. {@code "USER"} or {@code "ADMIN"}. */
    private String role;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** No-arg constructor required by JSON serialisers / deserialisers (e.g. Jackson). */
    public AuthResponse() {}

    /**
     * All-args constructor for building a response from the service result.
     *
     * @param accessToken  the short-lived JWT access token
     * @param refreshToken the long-lived JWT refresh token
     * @param email        the authenticated user's e-mail address
     * @param role         the authenticated user's authorisation role
     */
    public AuthResponse(String accessToken, String refreshToken,
                        String email, String role) {
        this.accessToken  = accessToken;
        this.refreshToken = refreshToken;
        this.email        = email;
        this.role         = role;
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    /**
     * Returns a human-readable representation of this response.
     * Token values are truncated to their first 20 characters to avoid
     * flooding logs with full JWT strings while still being identifiable.
     */
    @Override
    public String toString() {
        return "AuthResponse{" +
               "email='"        + email  + '\'' +
               ", role='"       + role   + '\'' +
               ", accessToken=" + truncate(accessToken)  +
               ", refreshToken=" + truncate(refreshToken) +
               '}';
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String truncate(String token) {
        if (token == null) return "null";
        return (token.length() > 20) ? token.substring(0, 20) + "…" : token;
    }
}
