package p2p.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import p2p.repository.StatsRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StatsHandler implements HttpHandler {

    private final StatsRepository statsRepository;

    public StatsHandler() {
        this.statsRepository = new StatsRepository();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            String email = (String) exchange.getAttribute("userEmail");

            long shared = statsRepository.getSharedCount(email);
            long received = statsRepository.getReceivedCount(email);
            long totalSharedSize = statsRepository.getTotalSharedSize(email);
            long totalReceivedSize = statsRepository.getTotalReceivedSize(email);

            String response = "{"
                    + "\"shared\":" + shared + ","
                    + "\"received\":" + received + ","
                    + "\"totalSharedSize\":" + totalSharedSize + ","
                    + "\"totalReceivedSize\":" + totalReceivedSize
                    + "}";

            exchange.getResponseHeaders().add("Content-Type", "application/json");

            exchange.sendResponseHeaders(
                    200,
                    response.getBytes(StandardCharsets.UTF_8).length
            );

            try(OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

        } catch(Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
