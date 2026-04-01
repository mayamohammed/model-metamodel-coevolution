package com.coevolution.dataset.impact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImpactAnalysisEngine {

    public enum ImpactLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }

    public static class ImpactReport {
        private final ImpactLevel          level;
        private final double               score;
        private final List<String>         affectedElements;
        private final Map<String, Integer> changeStats;
        private final String               recommendation;

        public ImpactReport(ImpactLevel level, double score,
                            List<String> affectedElements,
                            Map<String, Integer> changeStats,
                            String recommendation) {
            this.level            = level;
            this.score            = score;
            this.affectedElements = affectedElements;
            this.changeStats      = changeStats;
            this.recommendation   = recommendation;
        }

        public ImpactLevel          getLevel()            { return level;            }
        public double               getScore()            { return score;            }
        public List<String>         getAffectedElements() { return affectedElements; }
        public Map<String, Integer> getChangeStats()      { return changeStats;      }
        public String               getRecommendation()   { return recommendation;   }

        @Override
        public String toString() {
            return String.format(
                "ImpactReport{level=%s, score=%.2f, affected=%d, rec=%s}",
                level, score, affectedElements.size(), recommendation);
        }
    }

    private static final Map<String, Double> WEIGHTS = new HashMap<>();
    static {
        WEIGHTS.put("nb_removed_classes",      0.25);
        WEIGHTS.put("nb_removed_attributes",   0.20);
        WEIGHTS.put("nb_type_changes",         0.20);
        WEIGHTS.put("nb_removed_references",   0.10);
        WEIGHTS.put("nb_multiplicity_changes", 0.10);
        WEIGHTS.put("nb_containment_changes",  0.08);
        WEIGHTS.put("nb_supertype_changes",    0.05);
        WEIGHTS.put("nsuri_changed",           0.02);
    }

    public ImpactReport analyze(Map<String, Integer> features) {
        double      score      = computeScore(features);
        ImpactLevel level      = computeLevel(score, features);
        List<String> affected  = computeAffectedElements(features);
        String recommendation  = computeRecommendation(level, features);
        return new ImpactReport(level, score, affected, new HashMap<>(features), recommendation);
    }

    private double computeScore(Map<String, Integer> features) {
        double score = 0.0;
        int total = features.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        for (Map.Entry<String, Double> e : WEIGHTS.entrySet()) {
            int val = features.getOrDefault(e.getKey(), 0);
            if (val > 0) score += e.getValue() * Math.min(1.0, val / 5.0);
        }
        return Math.min(1.0, score);
    }

    private ImpactLevel computeLevel(double score, Map<String, Integer> features) {
        int removedClasses    = features.getOrDefault("nb_removed_classes", 0);
        int typeChanges       = features.getOrDefault("nb_type_changes", 0);
        int removedAttributes = features.getOrDefault("nb_removed_attributes", 0);

        if (removedClasses > 3 || (removedClasses > 0 && typeChanges > 0))
            return ImpactLevel.CRITICAL;
        if (removedClasses > 0 || typeChanges > 2 || removedAttributes > 3)
            return ImpactLevel.HIGH;
        if (score > 0.4 || typeChanges > 0 || removedAttributes > 0)
            return ImpactLevel.MEDIUM;
        if (score > 0.1)
            return ImpactLevel.LOW;
        return ImpactLevel.NONE;
    }

    private List<String> computeAffectedElements(Map<String, Integer> features) {
        List<String> affected = new ArrayList<>();
        if (features.getOrDefault("nb_removed_classes",      0) > 0) affected.add("EClass (removed)");
        if (features.getOrDefault("nb_added_classes",        0) > 0) affected.add("EClass (added)");
        if (features.getOrDefault("nb_removed_attributes",   0) > 0) affected.add("EAttribute (removed)");
        if (features.getOrDefault("nb_added_attributes",     0) > 0) affected.add("EAttribute (added)");
        if (features.getOrDefault("nb_type_changes",         0) > 0) affected.add("EAttribute.eType (changed)");
        if (features.getOrDefault("nb_removed_references",   0) > 0) affected.add("EReference (removed)");
        if (features.getOrDefault("nb_added_references",     0) > 0) affected.add("EReference (added)");
        if (features.getOrDefault("nb_multiplicity_changes", 0) > 0) affected.add("EReference.multiplicity (changed)");
        if (features.getOrDefault("nb_containment_changes",  0) > 0) affected.add("EReference.containment (changed)");
        if (features.getOrDefault("nb_abstract_changes",     0) > 0) affected.add("EClass.abstract (changed)");
        if (features.getOrDefault("nb_supertype_changes",    0) > 0) affected.add("EClass.eSuperTypes (changed)");
        if (features.getOrDefault("nsuri_changed",           0) > 0) affected.add("EPackage.nsURI (changed)");
        return affected;
    }

    private String computeRecommendation(ImpactLevel level, Map<String, Integer> features) {
        switch (level) {
            case NONE:     return "Aucun impact - migration automatique";
            case LOW:      return "Impact faible - migration automatique possible";
            case MEDIUM:   return "Impact modere - migration semi-automatique recommandee";
            case HIGH:     return "Impact eleve - migration manuelle requise";
            case CRITICAL: return "Impact critique - migration impossible sans refactoring manuel";
            default:       return "Impact inconnu";
        }
    }
}