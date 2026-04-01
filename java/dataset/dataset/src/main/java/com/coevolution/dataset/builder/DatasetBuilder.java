package com.coevolution.dataset.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetBuilder {

    
    public static class DatasetEntry {
        private final String              pairId;
        private final Map<String, Integer> features;
        private final String              label;

        public DatasetEntry(String pairId,
                            Map<String, Integer> features,
                            String label) {
            this.pairId   = pairId;
            this.features = new HashMap<>(features);
            this.label    = label;
        }

        public String              getPairId()   { return pairId;   }
        public Map<String, Integer> getFeatures() { return features; }
        public String              getLabel()    { return label;    }

        @Override
        public String toString() {
            return String.format("DatasetEntry{pairId=%s, label=%s, features=%d}",
                pairId, label, features.size());
        }
    }

    
    public static final List<String> FEATURE_NAMES = new ArrayList<>();
    static {
        FEATURE_NAMES.add("nb_added_classes");
        FEATURE_NAMES.add("nb_removed_classes");
        FEATURE_NAMES.add("nb_added_attributes");
        FEATURE_NAMES.add("nb_removed_attributes");
        FEATURE_NAMES.add("nb_type_changes");
        FEATURE_NAMES.add("nb_added_references");
        FEATURE_NAMES.add("nb_removed_references");
        FEATURE_NAMES.add("nb_multiplicity_changes");
        FEATURE_NAMES.add("nb_containment_changes");
        FEATURE_NAMES.add("nb_abstract_changes");
        FEATURE_NAMES.add("nb_supertype_changes");
        FEATURE_NAMES.add("nsuri_changed");
        FEATURE_NAMES.add("total_changes");
    }

    
    private final List<DatasetEntry> entries = new ArrayList<>();

    
    
    public void addEntry(String pairId,
                         Map<String, Integer> features,
                         String label) {
        Map<String, Integer> enriched = enrichFeatures(features);
        entries.add(new DatasetEntry(pairId, enriched, label));
    }

    
    private Map<String, Integer> enrichFeatures(Map<String, Integer> features) {
        Map<String, Integer> enriched = new HashMap<>(features);

        
        int total = 0;
        for (String key : FEATURE_NAMES) {
            if (!key.equals("total_changes")) {
                total += features.getOrDefault(key, 0);
            }
        }
        enriched.put("total_changes", total);

        
        for (String key : FEATURE_NAMES) {
            enriched.putIfAbsent(key, 0);
        }

        return enriched;
    }

    
    public List<DatasetEntry> getEntries()  { return entries; }
    public int                size()        { return entries.size(); }
    public boolean            isEmpty()     { return entries.isEmpty(); }

    
    public List<DatasetEntry> getEntriesByLabel(String label) {
        List<DatasetEntry> result = new ArrayList<>();
        for (DatasetEntry e : entries) {
            if (label.equals(e.getLabel())) result.add(e);
        }
        return result;
    }

    
    public Map<String, Integer> getLabelDistribution() {
        Map<String, Integer> dist = new HashMap<>();
        for (DatasetEntry e : entries) {
            dist.merge(e.getLabel(), 1, Integer::sum);
        }
        return dist;
    }

    
    public String getCsvHeader() {
        return String.join(",", FEATURE_NAMES) + ",label";
    }

    
    public String toCsvLine(DatasetEntry entry) {
        StringBuilder sb = new StringBuilder();
        for (String key : FEATURE_NAMES) {
            sb.append(entry.getFeatures().getOrDefault(key, 0)).append(",");
        }
        sb.append(entry.getLabel());
        return sb.toString();
    }

    
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCsvHeader()).append("\n");
        for (DatasetEntry e : entries) {
            sb.append(toCsvLine(e)).append("\n");
        }
        return sb.toString();
    }

    
    public void clear() {
        entries.clear();
    }

    @Override
    public String toString() {
        return String.format("DatasetBuilder{entries=%d, labels=%s}",
            entries.size(), getLabelDistribution());
    }
}