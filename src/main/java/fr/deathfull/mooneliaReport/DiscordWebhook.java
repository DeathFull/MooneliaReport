package fr.deathfull.mooneliaReport;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
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

    private static @NotNull HttpURLConnection getHttpURLConnection(String jsonPayload, URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("User-Agent", "MooneliaReport-Bot");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public void sendReport(String playerName, String message, String targetPlayer) throws IOException {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK")) {
            throw new IOException("Webhook URL non configurée");
        }

        JSONObject embed = new JSONObject();
        embed.put("title", "📢 Nouveau Report");
        embed.put("color", 15158332);
        embed.put("timestamp", Instant.now().toString());

        JSONObject author = new JSONObject();
        author.put("name", playerName);
        author.put("icon_url", "https://mc-heads.net/avatar/" + playerName);
        embed.put("author", author);

        JSONArray fields = new JSONArray();

        if (targetPlayer != null) {
            JSONObject targetField = new JSONObject();
            targetField.put("name", "🎯 Joueur ciblé");
            targetField.put("value", targetPlayer);
            targetField.put("inline", false);
            fields.add(targetField);

            JSONObject thumbnail = new JSONObject();
            thumbnail.put("url", "https://mc-heads.net/avatar/" + targetPlayer);
            embed.put("thumbnail", thumbnail);
        }

        JSONObject messageField = new JSONObject();
        messageField.put("name", "📝 Message");
        messageField.put("value", message);
        messageField.put("inline", false);
        fields.add(messageField);

        embed.put("fields", fields);

        JSONObject footer = new JSONObject();
        footer.put("text", "MooneliaReport");
        embed.put("footer", footer);

        JSONArray embeds = new JSONArray();
        embeds.add(embed);

        JSONObject payload = new JSONObject();
        payload.put("embeds", embeds);

        sendWebhook(payload.toJSONString());
    }

    private void sendWebhook(String jsonPayload) throws IOException {
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection connection = getHttpURLConnection(jsonPayload, url);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200 && responseCode != 204) {
            InputStream err = connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream();
            String body = "";
            if (err != null) {
                body = new String(err.readAllBytes(), StandardCharsets.UTF_8);
            }
            connection.disconnect();
            throw new IOException("Webhook returned response code: " + responseCode + " body: " + body);
        }

        connection.disconnect();
    }
}