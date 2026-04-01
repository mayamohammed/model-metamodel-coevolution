package com.coevolution.predictor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PredictionService
 *
 * Orchestrates metamodel change prediction by calling
 * the Flask API via FlaskApiClient.
 *
 * Semaine 6 - API + CLI
 */
public class PredictionService {

    private final FlaskApiClient client;

    // ── Constructors ──────────────────────────────────────────────
    public PredictionService(String apiBaseUrl) {
        this.client = new FlaskApiClient(apiBaseUrl);
    }

    public PredictionService() {
        this("http://localhost:5000");
    }

    // ── Single prediction ─────────────────────────────────────────
    public PredictionResult predict(FeatureVector features) {
        if (features == null) {
            throw new IllegalArgumentException("FeatureVector must not be null");
        }
        Map<String, Object> payload = features.toMap();
        Map<String, Object> response = client.postPredict(payload);
        return PredictionResult.fromMap(response);
    }

    // ── Batch prediction ──────────────────────────────────────────
    public List<PredictionResult> predictBatch(List<FeatureVector> featureList) {
        if (featureList == null || featureList.isEmpty()) {
            throw new IllegalArgumentException("Feature list must not be null or empty");
        }
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (FeatureVector fv : featureList) {
            items.add(fv.toMap());
        }
        return client.postPredictBatch(items);
    }

    // ── Health check ──────────────────────────────────────────────
    public boolean isApiHealthy() {
        return client.checkHealth();
    }

    // ── Model info ────────────────────────────────────────────────
    public Map<String, Object> getModelInfo() {
        return client.getModelInfo();
    }

    // ── Inner class: FeatureVector ────────────────────────────────
    public static class FeatureVector {
        private int nbAddedClasses;
        private int nbRemovedClasses;
        private int nbAddedAttributes;
        private int nbRemovedAttributes;
        private int nbTypeChanges;
        private int nbAddedReferences;
        private int nbRemovedReferences;
        private int nbMultiplicityChanges;
        private int nbContainmentChanges;
        private int nbAbstractChanges;
        private int nbSupertypeChanges;
        private int nsUriChanged;

        public FeatureVector() {}

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("nb_added_classes",       nbAddedClasses);
            map.put("nb_removed_classes",     nbRemovedClasses);
            map.put("nb_added_attributes",    nbAddedAttributes);
            map.put("nb_removed_attributes",  nbRemovedAttributes);
            map.put("nb_type_changes",        nbTypeChanges);
            map.put("nb_added_references",    nbAddedReferences);
            map.put("nb_removed_references",  nbRemovedReferences);
            map.put("nb_multiplicity_changes",nbMultiplicityChanges);
            map.put("nb_containment_changes", nbContainmentChanges);
            map.put("nb_abstract_changes",    nbAbstractChanges);
            map.put("nb_supertype_changes",   nbSupertypeChanges);
            map.put("nsuri_changed",          nsUriChanged);
            int total = nbAddedClasses + nbRemovedClasses
                      + nbAddedAttributes + nbRemovedAttributes
                      + nbTypeChanges + nbAddedReferences
                      + nbRemovedReferences + nbMultiplicityChanges
                      + nbContainmentChanges + nbAbstractChanges
                      + nbSupertypeChanges + nsUriChanged;
            map.put("total_changes", total);
            return map;
        }

        @Override
        public String toString() {
            return "FeatureVector{addedClasses=" + nbAddedClasses
                + ", removedClasses=" + nbRemovedClasses
                + ", addedAttrs=" + nbAddedAttributes
                + ", removedAttrs=" + nbRemovedAttributes
                + ", typeChanges=" + nbTypeChanges + "}";
        }

        // ── Getters & Setters ─────────────────────────────────────
        public int getNbAddedClasses()        { return nbAddedClasses; }
        public void setNbAddedClasses(int v)  { this.nbAddedClasses = v; }
        public int getNbRemovedClasses()      { return nbRemovedClasses; }
        public void setNbRemovedClasses(int v){ this.nbRemovedClasses = v; }
        public int getNbAddedAttributes()     { return nbAddedAttributes; }
        public void setNbAddedAttributes(int v){ this.nbAddedAttributes = v; }
        public int getNbRemovedAttributes()   { return nbRemovedAttributes; }
        public void setNbRemovedAttributes(int v){ this.nbRemovedAttributes = v; }
        public int getNbTypeChanges()         { return nbTypeChanges; }
        public void setNbTypeChanges(int v)   { this.nbTypeChanges = v; }
        public int getNbAddedReferences()     { return nbAddedReferences; }
        public void setNbAddedReferences(int v){ this.nbAddedReferences = v; }
        public int getNbRemovedReferences()   { return nbRemovedReferences; }
        public void setNbRemovedReferences(int v){ this.nbRemovedReferences = v; }
        public int getNbMultiplicityChanges() { return nbMultiplicityChanges; }
        public void setNbMultiplicityChanges(int v){ this.nbMultiplicityChanges = v; }
        public int getNbContainmentChanges()  { return nbContainmentChanges; }
        public void setNbContainmentChanges(int v){ this.nbContainmentChanges = v; }
        public int getNbAbstractChanges()     { return nbAbstractChanges; }
        public void setNbAbstractChanges(int v){ this.nbAbstractChanges = v; }
        public int getNbSupertypeChanges()    { return nbSupertypeChanges; }
        public void setNbSupertypeChanges(int v){ this.nbSupertypeChanges = v; }
        public int getNsUriChanged()          { return nsUriChanged; }
        public void setNsUriChanged(int v)    { this.nsUriChanged = v; }
    }

    // ── Inner class: PredictionResult ────────────────────────────
    public static class PredictionResult {
        private String prediction;
        private double confidence;
        private double confidencePct;
        private String error;
        private Map<String, Double> probabilities;

        public PredictionResult() {}

        @SuppressWarnings("unchecked")
        public static PredictionResult fromMap(Map<String, Object> map) {
            PredictionResult r = new PredictionResult();
            if (map.containsKey("error")) {
                r.error = String.valueOf(map.get("error"));
                return r;
            }
            r.prediction     = String.valueOf(map.getOrDefault("prediction", "UNKNOWN"));
            Object conf      = map.getOrDefault("confidence", 0.0);
            r.confidence     = conf instanceof Number ? ((Number) conf).doubleValue() : 0.0;
            r.confidencePct  = r.confidence * 100.0;
            if (map.containsKey("probabilities")) {
                Object p = map.get("probabilities");
                if (p instanceof Map) {
                    r.probabilities = new HashMap<>();
                    ((Map<?, ?>) p).forEach((k, v) -> {
                        if (v instanceof Number) {
                            r.probabilities.put(String.valueOf(k),
                                ((Number) v).doubleValue());
                        }
                    });
                }
            }
            return r;
        }

        public boolean isError()             { return error != null; }
        public String  getPrediction()       { return prediction; }
        public double  getConfidence()       { return confidence; }
        public double  getConfidencePct()    { return confidencePct; }
        public String  getError()            { return error; }
        public Map<String, Double> getProbabilities() { return probabilities; }

        @Override
        public String toString() {
            if (isError()) return "PredictionResult{error=" + error + "}";
            return String.format(
                "PredictionResult{prediction=%s, confidence=%.2f%%}",
                prediction, confidencePct);
        }
    }
}