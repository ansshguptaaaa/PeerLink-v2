package p2p.repository;

import p2p.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatsRepository {

    public long getSharedCount(String email) throws SQLException {

        String sql = "SELECT COUNT(*) FROM file_metadata WHERE owner_email = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }


    public long getReceivedCount(String email) throws SQLException {

        String sql = "SELECT COUNT(*) FROM file_transfers WHERE receiver_email = ?";

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }

    public long getTotalSharedSize(String email) throws SQLException {
        String sql = "SELECT COALESCE(SUM(file_size), 0) FROM file_metadata WHERE owner_email = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public long getTotalReceivedSize(String email) throws SQLException {
        String sql = "SELECT COALESCE(SUM(fm.file_size), 0) " +
                     "FROM file_transfers ft " +
                     "JOIN file_metadata fm ON ft.file_id = fm.id " +
                     "WHERE ft.receiver_email = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }
}
