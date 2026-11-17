package support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * A stand-in for the Trainer Workload service on a random local port (the JDK's own HTTP server), which
 * records what it receives and answers as told.
 */
public final class FakeWorkloadService implements AutoCloseable {

    /** One request as received. */
    public record Request(String method, String path, String authorization, String transactionId, String body) {
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String responseBody = "";
    private volatile long delayMillis;

    public FakeWorkloadService() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("X-Transaction-Id"), body));
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
        if (response.length > 0) {
            exchange.getResponseBody().write(response);
        }
        exchange.close();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respond(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    public void delay(long millis) {
        this.delayMillis = millis;
    }

    public List<Request> requests() {
        return List.copyOf(requests);
    }

    public void reset() {
        requests.clear();
        respond(200, "");
        delay(0);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
