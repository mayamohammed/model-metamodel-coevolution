package com.coevolution.analyzer.impact;

import com.coevolution.analyzer.features.FeatureVector;
import java.util.ArrayList;
import java.util.List;

public class MigrationErrorDetector {

    public static class MigrationError {
        public final String type;
        public final String message;
        public final String severity;
        public MigrationError(String type, String message, String severity) {
            this.type = type; this.message = message; this.severity = severity;
        }
        public String toString() {
            return "[" + severity + "] " + type + ": " + message;
        }
    }

    public List<MigrationError> detect(FeatureVector fv) {
        List<MigrationError> errors = new ArrayList<>();
        double[] v = fv.getValues();

        if (v[3] > 0)
            errors.add(new MigrationError("CLASS_ADDED",
                v[3] + " classe(s) ajoutee(s)", "INFO"));
        if (v[4] > 0)
            errors.add(new MigrationError("CLASS_REMOVED",
                v[4] + " classe(s) supprimee(s) - modeles orphelins", "ERROR"));
        if (v[8] > 0)
            errors.add(new MigrationError("ATTRIBUTE_ADDED",
                v[8] + " attribut(s) ajoute(s) - valeur defaut requise", "WARN"));
        if (v[9] > 0)
            errors.add(new MigrationError("ATTRIBUTE_REMOVED",
                v[9] + " attribut(s) supprime(s) - perte de donnees", "ERROR"));
        if (v[10] > 0)
            errors.add(new MigrationError("TYPE_CHANGED",
                v[10] + " type(s) change(s) - conversion requise", "ERROR"));
        if (v[16] > 0)
            errors.add(new MigrationError("MULTIPLICITY_CHANGED",
                v[16] + " multiplicite(s) changee(s)", "WARN"));

        return errors;
    }

    public static void main(String[] args) {
        System.out.println("=== MigrationErrorDetector Test ===");
        FeatureVector fv = new FeatureVector("test_pair",
            new double[]{3,4,1,1,0,9,12,3,1,0,1,3,3,0,0,0,1,0,0,0,0}, "MIXED");
        MigrationErrorDetector detector = new MigrationErrorDetector();
        List<MigrationError> errors = detector.detect(fv);
        System.out.println("  Erreurs detectees : " + errors.size());
        for (MigrationError e : errors)
            System.out.println("  " + e);
    }
}

