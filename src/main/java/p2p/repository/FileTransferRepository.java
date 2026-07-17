package p2p.repository;

import p2p.db.ConnectionManager;
import p2p.dto.TransferDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class FileTransferRepository {

    private static final String SQL_INSERT =
            "INSERT INTO file_transfers " +
            "(file_id, sender_email, receiver_email) VALUES (?, ?, ?)";

    public void save(long fileId, String senderEmail, String receiverEmail) throws SQLException {

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setLong(1, fileId);
            ps.setString(2, senderEmail);
            ps.setString(3, receiverEmail);

            ps.executeUpdate();
        }
    }

    public List<TransferDto> findTransfersByUser(String email) throws SQLException {
        List<TransferDto> list = new ArrayList<>();
        String sql = "SELECT ft.id, fm.file_name, ft.sender_email, ft.receiver_email, ft.downloaded_at, fm.file_size " +
                     "FROM file_transfers ft " +
                     "JOIN file_metadata fm ON ft.file_id = fm.id " +
                     "WHERE ft.sender_email = ? OR ft.receiver_email = ? " +
                     "ORDER BY ft.downloaded_at DESC";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TransferDto(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getString("sender_email"),
                        rs.getString("receiver_email"),
                        rs.getTimestamp("downloaded_at"),
                        rs.getLong("file_size")
                    ));
                }
            }
        }
        return list;
    }
}
