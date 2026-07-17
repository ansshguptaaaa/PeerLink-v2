package p2p.security;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A reusable decorator/wrapper HttpHandler that intercept requests to perform JWT authentication.
 *
 * <p>It reads the {@code Authorization} header, expects a {@code Bearer <token>} format,
 * validates the token, and extracts the subject (email). The email is then stored as an
 * attribute in the {@code HttpExchange} context. If validation fails or the header is missing,
 * it returns a {@code 401 Unauthorized} response.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * server.createContext("/protected-api", new JwtAuthHandler(new MyProtectedHandler()));
 * }</pre>
 */
public class JwtAuthHandler implements HttpHandler {

    private final HttpHandler next;

    /**
     * Creates a new JWT authentication filter wrapper around the provided next handler.
     *
     * @param next the next handler to invoke upon successful authentication
     */
    public JwtAuthHandler(HttpHandler next) {
        if (next == null) {
            throw new IllegalArgumentException("Next HttpHandler must not be null.");
        }
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle pre-flight CORS requests
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (JwtUtil.validateToken(token)) {
                try {
                    String email = JwtUtil.extractEmail(token);
                    exchange.setAttribute("userEmail", email);
                    
                    // Delegate to the wrapped protected handler
                    next.handle(exchange);
                    return;
                } catch (Exception e) {
                    System.err.println("Failed to extract details from valid JWT: " + e.getMessage());
                }
            }
        }

        // Return 401 Unauthorized if token is missing, invalid, or expired
        sendUnauthorizedResponse(exchange);
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        byte[] responseBytes = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(401, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
