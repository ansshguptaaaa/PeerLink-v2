package p2p.dto;

/**
 * Data Transfer Object representing the body of a login request.
 *
 * <p>Instances are typically deserialised from JSON by the HTTP layer and
 * passed directly to {@code AuthService.login(email, password)}.</p>
 *
 * <p><strong>Security note:</strong> {@link #toString()} deliberately omits
 * the {@code password} field to prevent credentials from leaking into logs.</p>
 */
public class LoginRequest {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** The user's registered e-mail address. */
    private String email;

    /**
     * The user's plaintext password.
     * Must be hashed before persistence; never log this value.
     */
    private String password;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** No-arg constructor required by JSON deserialisers (e.g. Jackson). */
    public LoginRequest() {}

    /**
     * All-args constructor for programmatic construction.
     *
     * @param email    the user's e-mail address
     * @param password the plaintext password
     */
    public LoginRequest(String email, String password) {
        this.email    = email;
        this.password = password;
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Returns the plaintext password. Handle with care — never log this value. */
    public String getPassword() {
        return password;
    }

    /** Sets the plaintext password. This value must be hashed before persistence. */
    public void setPassword(String password) {
        this.password = password;
    }

    // -----------------------------------------------------------------------
    // toString — password intentionally excluded
    // -----------------------------------------------------------------------

    /**
     * Returns a safe string representation.
     * The {@code password} field is <strong>excluded</strong> to prevent
     * credential leakage in application logs or stack traces.
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
               "email='" + email + '\'' +
               '}';
    }
}
