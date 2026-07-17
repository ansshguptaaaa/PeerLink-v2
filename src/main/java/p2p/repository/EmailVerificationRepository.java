package p2p.repository;

import p2p.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class EmailVerificationRepository {

    public static class VerificationRecord {
        private final String email;
        private final String otp;
        private final boolean verified;
        private final Timestamp expiresAt;
        private final Timestamp createdAt;

        public VerificationRecord(String email, String otp, boolean verified, Timestamp expiresAt, Timestamp createdAt) {
            this.email = email;
            this.otp = otp;
            this.verified = verified;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }

        public String getEmail() { return email; }
        public String getOtp() { return otp; }
        public boolean isVerified() { return verified; }
        public Timestamp getExpiresAt() { return expiresAt; }
        public Timestamp getCreatedAt() { return createdAt; }
    }

    public void saveOrUpdateOtp(String email, String otp, Timestamp expiresAt) throws SQLException {
        String sql = "INSERT INTO email_verifications (email, otp, verified, expires_at, created_at) " +
                     "VALUES (?, ?, FALSE, ?, NOW()) " +
                     "ON CONFLICT (email) DO UPDATE SET " +
                     "  otp = EXCLUDED.otp, " +
                     "  verified = FALSE, " +
                     "  expires_at = EXCLUDED.expires_at, " +
                     "  created_at = NOW()";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, otp);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        }
    }

    public VerificationRecord getVerification(String email) throws SQLException {
        String sql = "SELECT email, otp, verified, expires_at, created_at FROM email_verifications WHERE email = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new VerificationRecord(
                        rs.getString("email"),
                        rs.getString("otp"),
                        rs.getBoolean("verified"),
                        rs.getTimestamp("expires_at"),
                        rs.getTimestamp("created_at")
                    );
                }
            }
        }
        return null;
    }

    public void markAsVerified(String email) throws SQLException {
        String sql = "UPDATE email_verifications SET verified = TRUE WHERE email = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    public void deleteByEmail(String email) throws SQLException {
        String sql = "DELETE FROM email_verifications WHERE email = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }
}
