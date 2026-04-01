package com.coevolution.predictor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlaskApiClient {

    private final String baseUrl;
    private final int    timeoutMs;

    public FlaskApiClient(String baseUrl) {
        this.baseUrl   = baseUrl.replaceAll("/$", "");
        this.timeoutMs = 10_000;
    }

    public FlaskApiClient(String baseUrl, int timeoutMs) {
        this.baseUrl   = baseUrl.replaceAll("/$", "");
        this.timeoutMs = timeoutMs;
    }

    
    public boolean checkHealth() {
        try {
            String response = httpGet("/health");
            return response != null && response.contains("ok");
        } catch (Exception e) {
            System.err.println("[WARN] Health check failed: " + e.getMessage());
            return false;
        }
    }

    
    public Map<String, Object> getModelInfo() {
        try {
            String json = httpGet("/model/info");
            return parseJsonObject(json);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    
    public Map<String, Object> postPredict(Map<String, Object> features) {
        try {
            String body     = toJson(features);
            String response = httpPost("/predict", body);
            return parseJsonObject(response);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "HTTP error: " + e.getMessage());
            return err;
        }
    }

    
    public List<PredictionService.PredictionResult> postPredictBatch(
            List<Map<String, Object>> items) {
        List<PredictionService.PredictionResult> results = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(items.get(i)));
            }
            sb.append("]}");

            String response = httpPost("/predict/batch", sb.toString());
            Map<String, Object> resp = parseJsonObject(response);

            Object rawList = resp.get("results");
            if (rawList instanceof List) {
                for (Object item : (List<?>) rawList) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) item;
                        results.add(PredictionService.PredictionResult.fromMap(m));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Batch prediction failed: " + e.getMessage());
        }
        return results;
    }

    
    private String httpGet(String path) throws Exception {
        URL url = java.net.URI.create(baseUrl + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("GET " + path + " returned HTTP " + status);
        }
        return readResponse(conn);
    }

    
    private String httpPost(String path, String jsonBody) throws Exception {
        URL url = java.net.URI.create(baseUrl + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200 && status != 201 && status != 422) {
            throw new RuntimeException("POST " + path + " returned HTTP " + status);
        }
        return readResponse(conn);
    }

    
    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(),
                                      StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    
    static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else if (v instanceof Number) {
                sb.append(v);
            } else if (v instanceof Boolean) {
                sb.append(v);
            } else if (v == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(v).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    
    @SuppressWarnings("unchecked")
    static Map<String, Object> parseJsonObject(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null || json.isBlank()) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}"))  json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            
            while (i < json.length() &&
                   (json.charAt(i) == ',' || json.charAt(i) == ' ')) i++;
            if (i >= json.length()) break;

            
            if (json.charAt(i) != '"') { i++; continue; }
            int keyStart = i + 1;
            int keyEnd   = json.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key = json.substring(keyStart, keyEnd);
            i = keyEnd + 1;

            
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;
            while (i < json.length() && json.charAt(i) == ' ') i++;

            
            if (i >= json.length()) break;
            char c = json.charAt(i);
            if (c == '"') {
                int vs = i + 1;
                int ve = json.indexOf('"', vs);
                if (ve < 0) break;
                map.put(key, json.substring(vs, ve));
                i = ve + 1;
            } else if (c == '{') {
                int depth = 0; int start = i;
                while (i < json.length()) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                map.put(key, parseJsonObject(json.substring(start, i)));
            } else if (c == '[') {
                int depth = 0; int start = i;
                while (i < json.length()) {
                    if (json.charAt(i) == '[') depth++;
                    else if (json.charAt(i) == ']') { depth--; if (depth == 0) { i++; break; } }
                    i++;
                }
                map.put(key, json.substring(start, i));
            } else {
                int start = i;
                while (i < json.length() && json.charAt(i) != ','
                       && json.charAt(i) != '}') i++;
                String raw = json.substring(start, i).trim();
                if (raw.equals("true"))       map.put(key, Boolean.TRUE);
                else if (raw.equals("false")) map.put(key, Boolean.FALSE);
                else if (raw.equals("null"))  map.put(key, null);
                else {
                    try { map.put(key, Double.parseDouble(raw)); }
                    catch (NumberFormatException ex) { map.put(key, raw); }
                }
            }
        }
        return map;
    }

    public String getBaseUrl() { return baseUrl; }
}