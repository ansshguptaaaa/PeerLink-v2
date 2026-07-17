package p2p.model;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Domain model representing metadata of an uploaded file.
 */
public class FileMetadata {

    private Long id;
    private String ownerEmail;
    private String fileName;
    private int shareCode;
    private long fileSize;
    private Timestamp createdAt;

    public FileMetadata() {}

    public FileMetadata(String ownerEmail, String fileName, int shareCode) {
        this.ownerEmail = ownerEmail;
        this.fileName = fileName;
        this.shareCode = shareCode;
    }

    public FileMetadata(String ownerEmail, String fileName, int shareCode, long fileSize) {
        this.ownerEmail = ownerEmail;
        this.fileName = fileName;
        this.shareCode = shareCode;
        this.fileSize = fileSize;
    }

    public FileMetadata(Long id, String ownerEmail, String fileName, int shareCode, Timestamp createdAt) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.fileName = fileName;
        this.shareCode = shareCode;
        this.createdAt = createdAt;
    }

    public FileMetadata(Long id, String ownerEmail, String fileName, int shareCode, long fileSize, Timestamp createdAt) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.fileName = fileName;
        this.shareCode = shareCode;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getShareCode() {
        return shareCode;
    }

    public void setShareCode(int shareCode) {
        this.shareCode = shareCode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMetadata)) return false;
        FileMetadata that = (FileMetadata) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", fileName='" + fileName + '\'' +
                ", shareCode=" + shareCode +
                ", fileSize=" + fileSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
