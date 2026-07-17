package p2p.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Thread-safe singleton that owns the HikariCP connection pool for the PeerLink application.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Borrow a connection (auto-closes back to the pool via try-with-resources)
 * try (Connection conn = ConnectionManager.getInstance().getConnection()) {
 *     PreparedStatement ps = conn.prepareStatement("SELECT 1");
 *     ps.execute();
 * }
 *
 * // Shutdown (called once on application exit)
 * ConnectionManager.getInstance().close();
 * }</pre>
 *
 * <p>The singleton is initialised lazily on the first call to {@link #getInstance()} using
 * the <em>initialization-on-demand holder</em> idiom, which is both thread-safe and lock-free
 * after the first load.</p>
 */
public final class ConnectionManager implements AutoCloseable {

    static {
        // Fix: PostgreSQL rejects the deprecated JVM timezone alias "Asia/Calcutta".
        // Force the correct IANA name "Asia/Kolkata" at the JVM level so that the
        // PostgreSQL JDBC driver negotiates the connection parameters successfully.
        if ("Asia/Calcutta".equals(java.util.TimeZone.getDefault().getID())) {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        }
    }

    private final HikariDataSource dataSource;

    // -----------------------------------------------------------------------
    // Singleton – initialisation-on-demand holder idiom
    // -----------------------------------------------------------------------

    private static final class Holder {
        static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    /**
     * Returns the application-wide {@link ConnectionManager} instance.
     * The pool is created on the first call; subsequent calls return the same instance.
     *
     * @throws ExceptionInInitializerError if the pool cannot be initialised
     */
    public static ConnectionManager getInstance() {
        return Holder.INSTANCE;
    }

    // -----------------------------------------------------------------------
    // Constructor – private, called once by the Holder
    // -----------------------------------------------------------------------

    private ConnectionManager() {
        DatabaseConfig cfg = new DatabaseConfig();

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName(cfg.getPoolName());
        hikari.setJdbcUrl(cfg.getJdbcUrl());
        hikari.setUsername(cfg.getUsername());
        hikari.setPassword(cfg.getPassword());
        hikari.setDriverClassName(cfg.getDriverClassName());

        // Pool sizing
        hikari.setMinimumIdle(cfg.getMinimumIdle());
        hikari.setMaximumPoolSize(cfg.getMaximumPoolSize());

        // Timeouts
        hikari.setConnectionTimeout(cfg.getConnectionTimeoutMs());
        hikari.setIdleTimeout(cfg.getIdleTimeoutMs());
        hikari.setMaxLifetime(cfg.getMaxLifetimeMs());

        // Validate connections before handing them out
        hikari.setConnectionTestQuery("SELECT 1");

        // Auto-commit on by default; callers can disable per-connection
        hikari.setAutoCommit(true);

        // Fix: PostgreSQL rejects the deprecated JVM timezone alias "Asia/Calcutta".
        // Force the correct IANA name "Asia/Kolkata" at the session level on every
        // new connection so the server-side TimeZone parameter is always valid.
        hikari.setConnectionInitSql("SET TimeZone='Asia/Kolkata'");

        System.out.println("[ConnectionManager] Initialising pool: " + cfg);
        this.dataSource = new HikariDataSource(hikari);
        System.out.println("[ConnectionManager] Pool ready – max connections: "
                + cfg.getMaximumPoolSize());
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Borrows a live {@link Connection} from the pool.
     * <strong>Always</strong> use this inside a try-with-resources block so the connection is
     * returned to the pool automatically.
     *
     * @return an active JDBC {@link Connection}
     * @throws SQLException if no connection can be obtained within the configured timeout
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Returns the underlying {@link HikariDataSource} for advanced usage
     * (e.g. passing to a third-party library that expects a {@code DataSource}).
     *
     * @return the shared {@link HikariDataSource}
     */
    public HikariDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns {@code true} if the pool has been closed.
     */
    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    /**
     * Shuts down the connection pool gracefully, releasing all database connections.
     * Call this once during application shutdown.
     *
     * <p>Implements {@link AutoCloseable} so it can be used in try-with-resources at
     * the application level if desired.</p>
     */
    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            System.out.println("[ConnectionManager] Shutting down connection pool...");
            dataSource.close();
            System.out.println("[ConnectionManager] Pool shut down.");
        }
    }
}
