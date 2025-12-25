package com.hohseyuh.mcai.network;

import com.hohseyuh.mcai.Mcai;
import net.minecraft.server.MinecraftServer;
import java.net.http.*;
import java.net.URI;
import java.util.concurrent.*;

public class VoiceManager {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MinecraftServer server;

    public VoiceManager(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        // Poll every 500ms
        scheduler.scheduleAtFixedRate(this::pollServer, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void pollServer() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/get_command"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Assuming Python returns JSON: {"command": "job miner"}
            String body = response.body();
            if (body.contains("\"command\":\"") && !body.contains("\"command\":\"\"")) {
                String command = extractCommand(body);

                // CRITICAL: Switch to main server thread to touch entities
                server.execute(() -> {
                    Mcai.handleCommand(server.getPlayerManager().getPlayerList().get(0), command);
                });
            }
        } catch (Exception e) {
            // Server offline, ignore
        }
    }

    private String extractCommand(String json) {
        return json.split("\"command\":\"")[1].split("\"")[0];
    }

    public void stop() {
        scheduler.shutdown();
    }
}