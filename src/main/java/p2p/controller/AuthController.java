package p2p.controller;

import p2p.auth.AuthService;
import p2p.auth.AuthService.AuthException;
import p2p.auth.AuthService.AuthResponse;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * HTTP handler for the PeerLink authentication endpoints.
 *
 * <h2>Endpoints</h2>
 * <pre>
 * POST /signup   – register a new account
 * POST /login    – authenticate and receive tokens
 * POST /refresh  – exchange a refresh token for a new access token
 * </pre>
 *
 * <h2>Integration</h2>
 * <p>Obtain handler instances via the factory methods and register them with
 * the {@code HttpServer} in {@link FileController}:</p>
 * <pre>{@code
 * AuthController auth = new AuthController();
 * server.createContext("/signup",  auth.signupHandler());
 * server.createContext("/login",   auth.loginHandler());
 * server.createContext("/refresh", auth.refreshHandler());
 * }</pre>
 *
 * <h2>Thread-safety</h2>
 * <p>{@link AuthService} and all utilities it depends on are stateless or use
 * connection pooling; this class itself holds only a {@code final} reference
 * and is therefore safe to share across threads.</p>
 */
public class AuthController {

    private final AuthService authService;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** Creates an {@code AuthController} with a default {@link AuthService} instance. */
    public AuthController() {
        this(new AuthService());
    }

    /**
     * Creates an {@code AuthController} with an explicit {@link AuthService}.
     * Intended for dependency injection and unit testing.
     *
     * @param authService the service to delegate auth operations to
     */
    public AuthController(AuthService authService) {
        if (authService == null) {
            throw new IllegalArgumentException("authService must not be null.");
        }
        this.authService = authService;
    }

    // -----------------------------------------------------------------------
    // Handler factories
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link HttpHandler} for {@code POST /signup}.
     *
     * <p>Expected JSON body:</p>
     * <pre>{"username":"alice","email":"alice@example.com","password":"secret"}</pre>
     */
    public HttpHandler signupHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body   = readBody(exchange);
                System.out.println("REQUEST BODY = [" + body + "]");
                String username = extractJsonField(body, "username");
                String email    = extractJsonField(body, "email");
                String password = extractJsonField(body, "password");

                if (username == null || email == null || password == null) {
                    sendError(exchange, 400, "username, email and password are required.");
                    return;
                }

                AuthResponse result = authService.signup(username, email, password);
                sendJson(exchange, 201, buildAuthJson(result));

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    /**
     * Returns the {@link HttpHandler} for {@code POST /login}.
     *
     * <p>Expected JSON body:</p>
     * <pre>{"email":"alice@example.com","password":"secret"}</pre>
     */
    public HttpHandler loginHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body     = readBody(exchange);
                String email    = extractJsonField(body, "email");
                String password = extractJsonField(body, "password");

                if (email == null || password == null) {
                    sendError(exchange, 400, "email and password are required.");
                    return;
                }

                AuthResponse result = authService.login(email, password);
                sendJson(exchange, 200, buildAuthJson(result));

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    /**
     * Returns the {@link HttpHandler} for {@code POST /refresh}.
     *
     * <p>Expected JSON body:</p>
     * <pre>{"refreshToken":"&lt;token&gt;"}</pre>
     */
    public HttpHandler refreshHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body         = readBody(exchange);
                String refreshToken = extractJsonField(body, "refreshToken");

                if (refreshToken == null || refreshToken.isBlank()) {
                    sendError(exchange, 400, "refreshToken is required.");
                    return;
                }

                AuthResponse result = authService.refresh(refreshToken);
                sendJson(exchange, 200, buildAuthJson(result));

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    public HttpHandler sendOtpHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body   = readBody(exchange);
                String email  = extractJsonField(body, "email");

                if (email == null) {
                    sendError(exchange, 400, "email is required.");
                    return;
                }

                authService.sendOtp(email);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Verification code sent successfully.\"}");

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    public HttpHandler verifyOtpHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body  = readBody(exchange);
                String email = extractJsonField(body, "email");
                String otp   = extractJsonField(body, "otp");

                if (email == null || otp == null) {
                    sendError(exchange, 400, "email and otp are required.");
                    return;
                }

                authService.verifyOtp(email, otp);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Email verified successfully.\"}");

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    public HttpHandler forgotPasswordHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body  = readBody(exchange);
                String email = extractJsonField(body, "email");

                if (email == null) {
                    sendError(exchange, 400, "email is required.");
                    return;
                }

                authService.forgotPassword(email);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"If the email exists, a password reset OTP has been sent.\"}");

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    public HttpHandler verifyResetOtpHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body  = readBody(exchange);
                String email = extractJsonField(body, "email");
                String otp   = extractJsonField(body, "otp");

                if (email == null || otp == null) {
                    sendError(exchange, 400, "email and otp are required.");
                    return;
                }

                authService.verifyResetOtp(email, otp);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Verification code matches.\"}");

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    public HttpHandler resetPasswordHandler() {
        return exchange -> {
            setCorsHeaders(exchange);

            if (handlePreflight(exchange)) return;

            if (!isPost(exchange)) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body        = readBody(exchange);
                String email       = extractJsonField(body, "email");
                String otp         = extractJsonField(body, "otp");
                String newPassword = extractJsonField(body, "newPassword");

                if (email == null || otp == null || newPassword == null) {
                    sendError(exchange, 400, "email, otp and newPassword are required.");
                    return;
                }

                authService.resetPassword(email, otp, newPassword);
                sendJson(exchange, 200, "{\"success\":true,\"message\":\"Password updated successfully.\"}");

            } catch (AuthException e) {
                sendError(exchange, mapErrorCode(e.getErrorCode()), e.getMessage());
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        };
    }

    // -----------------------------------------------------------------------
    // Private helpers – CORS / request handling
    // -----------------------------------------------------------------------

    /** Adds CORS response headers to every exchange. */
    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Handles an HTTP OPTIONS pre-flight request.
     *
     * @return {@code true} if the request was a pre-flight and has been fully handled,
     *         {@code false} otherwise
     */
    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static boolean isPost(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    // -----------------------------------------------------------------------
    // Private helpers – I/O
    // -----------------------------------------------------------------------

    /** Reads the full request body as a UTF-8 string, stripping a leading UTF-8 BOM if present. */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // PowerShell Invoke-WebRequest / curl may prepend a UTF-8 BOM (U+FEFF)
            if (body.startsWith("\uFEFF")) {
                body = body.substring(1);
            }
            return body;
        }
    }

    /** Writes a JSON string with the given HTTP status code. */
    private static void sendJson(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Writes a plain-text error body wrapped in a JSON object. */
    private static void sendError(HttpExchange exchange, int status, String message)
            throws IOException {
        String escaped = message == null ? "" : message.replace("\"", "\\\"");
        String json = "{\"error\":\"" + escaped + "\"}";
        sendJson(exchange, status, json);
    }

    // -----------------------------------------------------------------------
    // Private helpers – JSON
    // -----------------------------------------------------------------------

    /**
     * Builds the standard auth JSON response from an {@link AuthResponse}.
     * Uses lightweight hand-built JSON to avoid pulling in a JSON library.
     */
    private static String buildAuthJson(AuthResponse r) {
        return "{"
             + "\"accessToken\":\""  + escape(r.getAccessToken())  + "\","
             + "\"refreshToken\":\"" + escape(r.getRefreshToken()) + "\","
             + "\"email\":\""        + escape(r.getEmail())        + "\","
             + "\"role\":\""         + escape(r.getRole())         + "\""
             + "}";
    }

    /**
     * Parses JSON request bodies (supporting both standard quoted JSON and loose unquoted formats
     * such as {username:ansh,email:ansh@test.com}) using a custom tokenizer combined with
     * Jackson's ObjectMapper for field mapping and retrieval.
     *
     * @param json the raw JSON string
     * @param key  the field name to extract
     * @return the string value of the key, or {@code null} if the key is not found
     */
    private static String extractJsonField(String json, String key) {
        if (json == null || json.isBlank()) return null;
        try {
            java.util.Map<String, String> map = parseLooseJson(json);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.valueToTree(map);
            JsonNode fieldNode = node.get(key);
            return (fieldNode == null || fieldNode.isNull()) ? null : fieldNode.asText();
        } catch (Exception e) {
            System.err.println("Error parsing JSON field: " + e.getMessage());
            return null;
        }
    }

    /**
     * A robust character-by-character tokenizer that parses a flat JSON object
     * with potential unquoted keys and unquoted string values.
     */
    private static java.util.Map<String, String> parseLooseJson(String json) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (json == null) return map;
        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        s = s.trim();

        int len = s.length();
        int i = 0;
        while (i < len) {
            // Read key
            StringBuilder keyBuilder = new StringBuilder();
            boolean insideQuotes = false;
            char quoteChar = 0;

            // Skip leading whitespace/commas
            while (i < len && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) {
                i++;
            }
            if (i >= len) break;

            if (s.charAt(i) == '"' || s.charAt(i) == '\'') {
                insideQuotes = true;
                quoteChar = s.charAt(i);
                i++;
            }

            while (i < len) {
                char c = s.charAt(i);
                if (insideQuotes) {
                    if (c == quoteChar) {
                        insideQuotes = false;
                        i++;
                        break;
                    }
                    keyBuilder.append(c);
                } else {
                    if (c == ':' || Character.isWhitespace(c)) {
                        break;
                    }
                    keyBuilder.append(c);
                }
                i++;
            }

            // Expect colon
            while (i < len && s.charAt(i) != ':') {
                i++;
            }
            if (i < len) i++; // skip colon

            // Skip whitespace after colon
            while (i < len && Character.isWhitespace(s.charAt(i))) {
                i++;
            }

            // Read value
            StringBuilder valBuilder = new StringBuilder();
            insideQuotes = false;
            quoteChar = 0;

            if (i < len && (s.charAt(i) == '"' || s.charAt(i) == '\'')) {
                insideQuotes = true;
                quoteChar = s.charAt(i);
                i++;
            }

            while (i < len) {
                char c = s.charAt(i);
                if (insideQuotes) {
                    // Handle escaped quotes inside value
                    if (c == '\\' && i + 1 < len && (s.charAt(i + 1) == quoteChar || s.charAt(i + 1) == '\\')) {
                        valBuilder.append(s.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (c == quoteChar) {
                        insideQuotes = false;
                        i++;
                        break;
                    }
                    valBuilder.append(c);
                } else {
                    if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                        break;
                    }
                    valBuilder.append(c);
                }
                i++;
            }

            map.put(keyBuilder.toString().trim(), valBuilder.toString().trim());
        }
        return map;
    }

    /** Escapes characters that are special inside a JSON string value. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    // -----------------------------------------------------------------------
    // Private helpers – error-code mapping
    // -----------------------------------------------------------------------

    /**
     * Maps a domain {@link AuthException.ErrorCode} to the appropriate HTTP status code.
     */
    private static int mapErrorCode(AuthException.ErrorCode code) {
        if (code == null) return 500;
        switch (code) {
            case EMAIL_ALREADY_EXISTS:  return 409; // Conflict
            case USER_NOT_FOUND:        return 404;
            case INVALID_CREDENTIALS:   return 401; // Unauthorized
            case INVALID_REFRESH_TOKEN: return 401;
            case DATABASE_ERROR:        return 503; // Service Unavailable
            case INVALID_OTP:           return 400;
            case OTP_EXPIRED:           return 400;
            case TOO_MANY_OTP_REQUESTS: return 429;
            default:                    return 500;
        }
    }
}
