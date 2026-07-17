package p2p.dto;

/**
 * Data Transfer Object representing the body of a user-registration request.
 *
 * <p>Instances are typically deserialised from JSON by the HTTP layer and
 * passed directly to {@code AuthService.signup(username, email, password)}.</p>
 *
 * <p><strong>Security note:</strong> {@link #toString()} deliberately omits
 * the {@code password} field to prevent credentials from leaking into logs.</p>
 */
public class SignupRequest {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** The desired unique display name / login handle. */
    private String username;

    /** The user's e-mail address; must be unique across all accounts. */
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
    public SignupRequest() {}

    /**
     * All-args constructor for programmatic construction.
     *
     * @param username the desired login handle
     * @param email    the user's e-mail address
     * @param password the plaintext password
     */
    public SignupRequest(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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
        return "SignupRequest{" +
               "username='" + username + '\'' +
               ", email='"  + email    + '\'' +
               '}';
    }
}
