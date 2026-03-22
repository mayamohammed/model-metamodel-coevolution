package com.coevolution.analyzer.impact;

import com.coevolution.analyzer.features.FeatureVector;

public class ImpactAnalysisEngine {

    public enum ImpactLevel { LOW, MEDIUM, HIGH, CRITICAL }

    public static class ImpactReport {
        public final ImpactLevel level;
        public final int score;
        public final String summary;
        public ImpactReport(ImpactLevel level, int score, String summary) {
            this.level = level; this.score = score; this.summary = summary;
        }
        public String toString() {
            return "Impact=" + level + " (score=" + score + ") : " + summary;
        }
    }

    public ImpactReport analyze(FeatureVector fv) {
        double[] v = fv.getValues();
        int score = 0;

        score += (int)(v[4]  * 10);
        score += (int)(v[9]  * 5);
        score += (int)(v[10] * 8);
        score += (int)(v[3]  * 2);
        score += (int)(v[8]  * 2);
        score += (int)(v[16] * 3);
        score += (int)(v[18] * 4);
        score += (int)(v[19] * 6);
        score += (int)(v[20] * 15);

        ImpactLevel level;
        String summary;
        if (score == 0) {
            level = ImpactLevel.LOW;
            summary = "Aucun changement detecte";
        } else if (score <= 5) {
            level = ImpactLevel.LOW;
            summary = "Changements mineurs - migration simple";
        } else if (score <= 15) {
            level = ImpactLevel.MEDIUM;
            summary = "Changements moderés - migration assistée recommandée";
        } else if (score <= 30) {
            level = ImpactLevel.HIGH;
            summary = "Changements importants - transformation ATL requise";
        } else {
            level = ImpactLevel.CRITICAL;
            summary = "Changements critiques - migration manuelle nécessaire";
        }

        return new ImpactReport(level, score, summary);
    }

    public static void main(String[] args) {
        System.out.println("=== ImpactAnalysisEngine Test ===");
        ImpactAnalysisEngine engine = new ImpactAnalysisEngine();
        FeatureVector fv = new FeatureVector("test_pair",
            new double[]{3,4,1,1,0,9,12,3,1,0,1,3,3,0,0,0,1,0,0,0,0}, "MIXED");
        System.out.println("  " + engine.analyze(fv));
    }
}

