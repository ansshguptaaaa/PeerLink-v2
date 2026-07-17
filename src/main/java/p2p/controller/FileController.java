package p2p.controller;

import p2p.controller.AuthController;
import p2p.service.FileSharer;
import p2p.security.JwtAuthHandler;
import p2p.model.FileMetadata;
import p2p.repository.FileMetadataRepository;
import p2p.repository.FileTransferRepository;
import p2p.repository.StatsRepository;



import java.io.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

import org.apache.commons.io.IOUtils;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;
    private final AuthController authController;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileTransferRepository fileTransferRepository;
    private final StatsRepository statsRepository;


    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);
        this.authController = new AuthController();
        this.fileMetadataRepository = new FileMetadataRepository();
        this.fileTransferRepository = new FileTransferRepository();
        this.statsRepository = new StatsRepository();
        
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        // Existing P2P routes
        server.createContext("/upload",   new JwtAuthHandler(new UploadHandler()));
        server.createContext("/download", new JwtAuthHandler(new DownloadHandler()));
        server.createContext("/files",    new JwtAuthHandler(new FilesHandler()));
        server.createContext("/stats",    new JwtAuthHandler(new StatsHandler()));
        server.createContext("/transfers", new JwtAuthHandler(new TransfersHandler()));
        server.createContext("/",         new CORSHandler());

        // Authentication routes
        server.createContext("/signup",  authController.signupHandler());
        server.createContext("/login",   authController.loginHandler());
        server.createContext("/refresh", authController.refreshHandler());
        server.createContext("/send-otp",   authController.sendOtpHandler());
        server.createContext("/verify-otp", authController.verifyOtpHandler());
        server.createContext("/forgot-password", authController.forgotPasswordHandler());
        server.createContext("/verify-reset-otp", authController.verifyResetOtpHandler());
        server.createContext("/reset-password", authController.resetPasswordHandler());
        
        server.setExecutor(executorService);
    }
    
    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
    
    private class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private static class MultipartParser {
        private final byte[] data;
        private final String boundary;
        
        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }
        
        public ParseResult parse() {
            try {
                String dataAsString = new String(data);
                
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker);
                if (filenameStart == -1) {
                    return null;
                }
                
                filenameStart += filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);
                
                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream"; // Default
                
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }
                
                String headerEndMarker = "\r\n\r\n";
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }
                
                int contentStart = headerEnd + headerEndMarker.length();
                
                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);
                
                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }
                
                if (contentEnd == -1 || contentEnd <= contentStart) {
                    return null;
                }
                
                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);
                
                return new ParseResult(filename, contentType, fileContent);
            } catch (Exception e) {
                System.err.println("Error parsing multipart data: " + e.getMessage());
                return null;
            }
        }
        
        private int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer:
            for (int i = startPos; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }
        
        public static class ParseResult {
            public final String filename;
            public final String contentType;
            public final byte[] fileContent;
            
            public ParseResult(String filename, String contentType, byte[] fileContent) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }
    
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");
            
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();
                
                MultipartParser parser = new MultipartParser(requestData, boundary);
                MultipartParser.ParseResult result = parser.parse();
                
                if (result == null) {
                    String response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }
                
                String filename = result.filename;
                if (filename == null || filename.trim().isEmpty()) {
                    filename = "unnamed-file";
                }
                
                String uniqueFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                String filePath = uploadDir + File.separator + uniqueFilename;
                
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);
                }
                
                int port = fileSharer.offerFile(filePath);
                
                // Save metadata in PostgreSQL
                String ownerEmail = (String) exchange.getAttribute("userEmail");
                if (ownerEmail == null) {
                    ownerEmail = "anonymous";
                }
                FileMetadata metadata = new FileMetadata(ownerEmail, filename, port, result.fileContent.length);
                fileMetadataRepository.save(metadata);
                
                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
                
            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
    
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/') + 1);

            try {
                int code = Integer.parseInt(portStr);
                
                // Verify file exists in metadata in PostgreSQL
                if (!fileMetadataRepository.existsByShareCode(code)) {
                    String response = "Not Found: No file metadata associated with code " + code;
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                String filePath = fileSharer.getFilePath(code);
                if (filePath == null) {
                    String response = "Not Found: No file associated with code " + code;
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                File file = new File(filePath);
                if (!file.exists()) {
                    String response = "Not Found: File no longer available";
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                // Extract original filename (stored as uuid_originalname)
                String storedName = file.getName();
                int underscoreIdx = storedName.indexOf('_');
                String filename = (underscoreIdx != -1 && underscoreIdx < storedName.length() - 1)
                        ? storedName.substring(underscoreIdx + 1)
                        : storedName;

                headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                headers.add("Content-Type", "application/octet-stream");

                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }


                String receiverEmail = (String) exchange.getAttribute("userEmail");

                fileMetadataRepository.findByShareCode(code)
                    .ifPresent(metadata -> {
                        try {
                            fileTransferRepository.save(
                                metadata.getId(),
                                metadata.getOwnerEmail(),
                                receiverEmail
                            );
                        } catch (Exception ex) {
                            System.err.println("Transfer save failed: " + ex.getMessage());
                        }
                    });

            } catch (NumberFormatException e) {
                String response = "Bad Request: Invalid download code";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                System.err.println("Error processing file download: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private class FilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Content-Type", "application/json; charset=UTF-8");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "{\"error\":\"Method Not Allowed\"}";
                exchange.sendResponseHeaders(405, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                return;
            }
            
            try {
                String ownerEmail = (String) exchange.getAttribute("userEmail");
                if (ownerEmail == null) {
                    ownerEmail = "anonymous";
                }
                
                java.util.List<FileMetadata> files = fileMetadataRepository.findByOwnerEmail(ownerEmail);
                
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < files.size(); i++) {
                    FileMetadata f = files.get(i);
                    sb.append("{");
                    sb.append("\"id\":").append(f.getId()).append(",");
                    sb.append("\"fileName\":\"").append(escapeJson(f.getFileName())).append("\",");
                    sb.append("\"shareCode\":").append(f.getShareCode()).append(",");
                    sb.append("\"fileSize\":").append(f.getFileSize()).append(",");
                    sb.append("\"createdAt\":\"").append(formatTimestamp(f.getCreatedAt())).append("\"");
                    sb.append("}");
                    if (i < files.size() - 1) {
                        sb.append(",");
                    }
                }
                sb.append("]");
                
                byte[] responseBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                System.err.println("Error fetching files: " + e.getMessage());
                String response = "{\"error\":\"Server error: " + escapeJson(e.getMessage()) + "\"}";
                exchange.sendResponseHeaders(500, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        
        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
        
        private String formatTimestamp(java.sql.Timestamp t) {
            if (t == null) return "";
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return t.toLocalDateTime().format(formatter);
        }
    }

    private class TransfersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Content-Type", "application/json; charset=UTF-8");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "{\"error\":\"Method Not Allowed\"}";
                exchange.sendResponseHeaders(405, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                return;
            }
            
            try {
                String email = (String) exchange.getAttribute("userEmail");
                if (email == null) {
                    email = "anonymous";
                }
                
                java.util.List<p2p.dto.TransferDto> transfers = fileTransferRepository.findTransfersByUser(email);
                
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                for (int i = 0; i < transfers.size(); i++) {
                    p2p.dto.TransferDto t = transfers.get(i);
                    sb.append("{");
                    sb.append("\"id\":").append(t.getId()).append(",");
                    sb.append("\"fileName\":\"").append(escapeJson(t.getFileName())).append("\",");
                    sb.append("\"senderEmail\":\"").append(escapeJson(t.getSenderEmail())).append("\",");
                    sb.append("\"receiverEmail\":\"").append(escapeJson(t.getReceiverEmail())).append("\",");
                    sb.append("\"fileSize\":").append(t.getFileSize()).append(",");
                    sb.append("\"downloadedAt\":\"").append(formatTimestamp(t.getDownloadedAt())).append("\"");
                    sb.append("}");
                    if (i < transfers.size() - 1) {
                        sb.append(",");
                    }
                }
                sb.append("]");
                
                byte[] responseBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                System.err.println("Error fetching transfers: " + e.getMessage());
                String response = "{\"error\":\"Server error: " + escapeJson(e.getMessage()) + "\"}";
                exchange.sendResponseHeaders(500, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        
        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
        
        private String formatTimestamp(java.sql.Timestamp t) {
            if (t == null) return "";
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return t.toLocalDateTime().format(formatter);
        }
    }
}




