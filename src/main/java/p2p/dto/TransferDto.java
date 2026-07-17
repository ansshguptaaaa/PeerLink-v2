package p2p.dto;

import java.sql.Timestamp;

public class TransferDto {
    private long id;
    private String fileName;
    private String senderEmail;
    private String receiverEmail;
    private Timestamp downloadedAt;
    private long fileSize;

    public TransferDto() {}

    public TransferDto(long id, String fileName, String senderEmail, String receiverEmail, Timestamp downloadedAt, long fileSize) {
        this.id = id;
        this.fileName = fileName;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.downloadedAt = downloadedAt;
        this.fileSize = fileSize;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public Timestamp getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(Timestamp downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
