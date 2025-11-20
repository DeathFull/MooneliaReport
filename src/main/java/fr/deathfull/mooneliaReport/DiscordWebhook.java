package fr.deathfull.mooneliaReport;

import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordWebhook {

    private final String webhookUrl;

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendReport(String playerName, String message) throws IOException {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            throw new IOException("Webhook URL non configurée");
        }

        JSONObject embed = new JSONObject();
        embed.put("title", "📢 Nouveau Report");
        embed.put("description", message);
        embed.put("color", 15158332); // Rouge
        embed.put("timestamp", Instant.now().toString());

        JSONObject author = new JSONObject();
        author.put("name", playerName);
        embed.put("author", author);

        JSONObject footer = new JSONObject();
        footer.put("text", "MooneliaReport");
        embed.put("footer", footer);

        JSONObject payload = new JSONObject();
        payload.put("embeds", new JSONObject[]{embed});

        sendWebhook(payload.toJSONString());
    }

    /**
     * Envoie la requête HTTP au webhook Discord
     */
    private void sendWebhook(String jsonPayload) throws IOException {
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "MooneliaReport-Bot");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200 && responseCode != 204) {
            throw new IOException("Webhook returned response code: " + responseCode);
        }

        connection.disconnect();
    }
}

