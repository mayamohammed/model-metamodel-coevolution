package com.coevolution.gui.services;

import com.coevolution.predictor.FlaskApiClient;
import java.util.Map;

/**
 * PredictionGuiService
 * Wraps FlaskApiClient for GUI usage.
 * Semaine 8 - GUI
 */
public class PredictionGuiService {

    private final FlaskApiClient client;

    public PredictionGuiService(String apiUrl) {
        this.client = new FlaskApiClient(apiUrl);
    }

    public boolean isHealthy() {
        return client.checkHealth();
    }

    public Map<String, Object> predict(Map<String, Integer> features) {
        Map<String, Object> payload = new java.util.HashMap<>(features);
        int total = features.values().stream().mapToInt(Integer::intValue).sum();
        payload.put("total_changes", total);
        return client.postPredict(payload);
    }

    public Map<String, Object> getModelInfo() {
        return client.getModelInfo();
    }
}