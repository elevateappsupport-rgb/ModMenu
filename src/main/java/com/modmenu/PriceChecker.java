package com.modmenu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Looks up DonutSMP auction prices through the public DonutSMP API (needs your own API key from /api). */
public class PriceChecker {
    public static volatile List<String> lines = List.of("Enter your API key and an item, then press Search.");

    private static final String BASE = "https://api.donutsmp.net/v1";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void search(String key, String query) {
        String q = query == null ? "" : query.trim();
        if (key == null || key.isBlank()) { lines = List.of("Please enter your API key first (/api on DonutSMP)."); return; }
        if (q.isEmpty()) { lines = List.of("Please enter an item name."); return; }
        lines = List.of("Searching...");
        String norm = q.toLowerCase(Locale.ROOT).replace(' ', '_');

        CompletableFuture.runAsync(() -> {
            try {
                List<Double> listed = prices(call(key, "/auction/list/1", "{\"search\":\"" + norm + "\",\"sort\":\"lowest_price\"}"), norm);
                List<Double> sold = new ArrayList<>();
                try { sold = prices(call(key, "/auction/transactions/1", "{\"search\":\"" + norm + "\"}"), norm); } catch (Exception ignored) {}

                List<String> out = new ArrayList<>();
                out.add("Item: " + q + " (prices per item)");
                if (listed.isEmpty()) {
                    out.add("No current listings found on page 1.");
                } else {
                    Collections.sort(listed);
                    double cheapest = listed.get(0), median = listed.get(listed.size() / 2);
                    out.add("Listings found: " + listed.size());
                    out.add("Cheapest now: $" + money(cheapest));
                    out.add("Median listing: $" + money(median));
                    double resell = median;
                    if (!sold.isEmpty()) {
                        Collections.sort(sold);
                        double soldMedian = sold.get(sold.size() / 2);
                        out.add("Recently sold (median): $" + money(soldMedian) + " (" + sold.size() + " sales)");
                        resell = Math.min(median, soldMedian);
                    }
                    out.add("Buy: at or below $" + money(cheapest * 1.02));
                    out.add("Resell: around $" + money(resell * 0.97));
                    out.add("Estimated profit per item: $" + money(resell * 0.97 - cheapest * 1.02) + " (before any fees)");
                    if (resell * 0.97 <= cheapest * 1.02) out.add("-> Not worth flipping right now.");
                }
                out.add("Based on the first page of results only.");
                lines = out;
            } catch (Exception e) {
                lines = List.of("Error: " + e.getMessage(), "Check your API key and internet connection.");
            }
        });
    }

    private static String money(double v) { return String.format(Locale.ROOT, "%,.0f", v); }

    private static String call(String key, String path, String body) throws Exception {
        HttpRequest post = base(key, path).POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json").build();
        HttpResponse<String> r = HTTP.send(post, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() == 404 || r.statusCode() == 405) {
            r = HTTP.send(base(key, path).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        if (r.statusCode() == 401 || r.statusCode() == 403) throw new Exception("API key rejected (HTTP " + r.statusCode() + ")");
        if (r.statusCode() / 100 != 2) throw new Exception("HTTP " + r.statusCode());
        return r.body();
    }

    private static HttpRequest.Builder base(String key, String path) {
        return HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15));
    }

    private static List<Double> prices(String json, String norm) {
        List<Double> out = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement res = root.get("result");
        if (res == null || !res.isJsonArray()) return out;
        JsonArray arr = res.getAsJsonArray();
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                JsonObject item = o.getAsJsonObject("item");
                String id = item.get("id").getAsString().toLowerCase(Locale.ROOT);
                if (!id.contains(norm)) continue;
                int count = item.has("count") ? Math.max(1, item.get("count").getAsInt()) : 1;
                double price = o.get("price").getAsDouble();
                out.add(price / count);
            } catch (Exception ignored) {}
        }
        return out;
    }
}
