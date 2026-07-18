package p2p.db;

/**
 * Holds all PostgreSQL / HikariCP configuration parameters for the PeerLink application.
 *
 * <p>Values are read from system properties first (so they can be overridden at runtime via
 * {@code -Ddb.url=...}), and fall back to the compile-time defaults below.</p>
 *
 * <pre>
 * Override examples:
 *   java -Ddb.url=jdbc:postgresql://prod-host:5432/peerlink -jar app.jar
 *   java -Ddb.username=myuser -Ddb.password=secret -jar app.jar
 * </pre>
 */
public final class DatabaseConfig {

    // -----------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------
    private static final String DEFAULT_JDBC_URL  = "jdbc:postgresql://localhost:5432/peerlink";
    private static final String DEFAULT_USERNAME  = "postgres";
    private static final String DEFAULT_PASSWORD  = "root123";
    private static final String DEFAULT_DRIVER    = "org.postgresql.Driver";

    // HikariCP pool defaults
    private static final int DEFAULT_MIN_IDLE              = 2;
    private static final int DEFAULT_MAX_POOL_SIZE         = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L;  // 30 s
    private static final long DEFAULT_IDLE_TIMEOUT_MS       = 600_000L; // 10 min
    private static final long DEFAULT_MAX_LIFETIME_MS       = 1_800_000L; // 30 min
    private static final String DEFAULT_POOL_NAME           = "PeerLink-DB-Pool";

    // -----------------------------------------------------------------------
    // Resolved values
    // -----------------------------------------------------------------------
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final int minimumIdle;
    private final int maximumPoolSize;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final String poolName;

    /**
     * Creates a {@code DatabaseConfig} by resolving system properties with compile-time fallbacks.
     */
    public DatabaseConfig() {
        this.jdbcUrl = System.getenv().getOrDefault("DB_URL", System.getProperty("db.url", DEFAULT_JDBC_URL));
        this.username = System.getenv().getOrDefault("DB_USERNAME", System.getProperty("db.username", DEFAULT_USERNAME));
        this.password = System.getenv().getOrDefault("DB_PASSWORD", System.getProperty("db.password", DEFAULT_PASSWORD));
        this.driverClassName     = System.getProperty("db.driver",   DEFAULT_DRIVER);
        this.poolName            = System.getProperty("db.poolName", DEFAULT_POOL_NAME);

        this.minimumIdle         = intProp("db.minIdle",         DEFAULT_MIN_IDLE);
        this.maximumPoolSize     = intProp("db.maxPoolSize",     DEFAULT_MAX_POOL_SIZE);
        this.connectionTimeoutMs = longProp("db.connectionTimeout", DEFAULT_CONNECTION_TIMEOUT_MS);
        this.idleTimeoutMs       = longProp("db.idleTimeout",       DEFAULT_IDLE_TIMEOUT_MS);
        this.maxLifetimeMs       = longProp("db.maxLifetime",       DEFAULT_MAX_LIFETIME_MS);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getJdbcUrl()             { return jdbcUrl; }
    public String getUsername()            { return username; }
    public String getPassword()            { return password; }
    public String getDriverClassName()     { return driverClassName; }
    public int    getMinimumIdle()         { return minimumIdle; }
    public int    getMaximumPoolSize()     { return maximumPoolSize; }
    public long   getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public long   getIdleTimeoutMs()       { return idleTimeoutMs; }
    public long   getMaxLifetimeMs()       { return maxLifetimeMs; }
    public String getPoolName()            { return poolName; }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static int intProp(String key, int defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) return defaultValue;
        try { return Integer.parseInt(val.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static long longProp(String key, long defaultValue) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) return defaultValue;
        try { return Long.parseLong(val.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
               "url='" + jdbcUrl + '\'' +
               ", username='" + username + '\'' +
               ", pool='" + poolName + '\'' +
               ", minIdle=" + minimumIdle +
               ", maxPoolSize=" + maximumPoolSize +
               '}';
    }
}

