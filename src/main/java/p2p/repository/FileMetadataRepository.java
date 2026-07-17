package p2p.repository;

import p2p.db.ConnectionManager;
import p2p.model.FileMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

/**
 * JDBC repository for storing and querying {@link FileMetadata} records in PostgreSQL.
 *
 * <h2>Required DDL</h2>
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS file_metadata (
 *     id           BIGSERIAL PRIMARY KEY,
 *     owner_email  VARCHAR(255) NOT NULL,
 *     file_name    VARCHAR(255) NOT NULL,
 *     share_code   INTEGER NOT NULL UNIQUE,
 *     created_at   TIMESTAMP NOT NULL DEFAULT NOW()
 * );
 * CREATE INDEX IF NOT EXISTS idx_file_metadata_share_code ON file_metadata(share_code);
 * }</pre>
 */
public class FileMetadataRepository {

    private static final String SQL_INSERT =
            "INSERT INTO file_metadata (owner_email, file_name, share_code, created_at, file_size) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id";

    private static final String SQL_FIND_BY_SHARE_CODE =
            "SELECT id, owner_email, file_name, share_code, file_size, created_at " +
            "FROM file_metadata WHERE share_code = ?";

    private static final String SQL_EXISTS_BY_SHARE_CODE =
            "SELECT 1 FROM file_metadata WHERE share_code = ? LIMIT 1";

    public FileMetadataRepository() {
        // stateless
    }

    /**
     * Persists new file metadata.
     *
     * @param metadata file metadata to save
     * @throws SQLException if database error occurs
     */
    public void save(FileMetadata metadata) throws SQLException {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null.");
        }

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, metadata.getOwnerEmail());
            ps.setString(2, metadata.getFileName());
            ps.setInt(3, metadata.getShareCode());
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.setLong(5, metadata.getFileSize());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    metadata.setId(rs.getLong("id"));
                }
            }
        }
    }

    /**
     * Finds file metadata by share code.
     *
     * @param shareCode the code to search for
     * @return an Optional of FileMetadata
     * @throws SQLException if query fails
     */
    public Optional<FileMetadata> findByShareCode(int shareCode) throws SQLException {
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_SHARE_CODE)) {

            ps.setInt(1, shareCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new FileMetadata(
                            rs.getLong("id"),
                            rs.getString("owner_email"),
                            rs.getString("file_name"),
                            rs.getInt("share_code"),
                            rs.getLong("file_size"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if file metadata exists for a given share code.
     *
     * @param shareCode the code to check
     * @return true if exists, false otherwise
     * @throws SQLException if query fails
     */
    public boolean existsByShareCode(int shareCode) throws SQLException {
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_SHARE_CODE)) {

            ps.setInt(1, shareCode);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Finds all file metadata records belonging to an owner email, ordered by created_at DESC.
     *
     * @param email the owner email to search for
     * @return a list of FileMetadata records
     * @throws SQLException if database query fails
     */
    public List<FileMetadata> findByOwnerEmail(String email) throws SQLException {
        List<FileMetadata> list = new ArrayList<>();
        String sql = "SELECT id, owner_email, file_name, share_code, file_size, created_at " +
                     "FROM file_metadata WHERE owner_email = ? ORDER BY created_at DESC";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FileMetadata(
                            rs.getLong("id"),
                            rs.getString("owner_email"),
                            rs.getString("file_name"),
                            rs.getInt("share_code"),
                            rs.getLong("file_size"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return list;
    }
}
