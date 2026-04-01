package com.coevolution.cli.commands;

import com.coevolution.predictor.FlaskApiClient;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DiffCommand {

    private final FlaskApiClient client;

    public DiffCommand(String apiUrl) {
        this.client = new FlaskApiClient(apiUrl);
    }

    public void execute(String[] args) {
        String v1Path = null, v2Path = null;

        for (int i = 1; i < args.length - 1; i++) {
            switch (args[i].toLowerCase()) {
                case "--v1": v1Path = args[i + 1]; i++; break;
                case "--v2": v2Path = args[i + 1]; i++; break;
            }
        }

        if (v1Path == null || v2Path == null) {
            System.err.println("[ERROR] Usage: diff --v1 <file1.ecore> --v2 <file2.ecore>");
            System.exit(1);
        }

        File f1 = new File(v1Path);
        File f2 = new File(v2Path);

        if (!f1.exists()) {
            System.err.println("[ERROR] File not found: " + v1Path); System.exit(1); }
        if (!f2.exists()) {
            System.err.println("[ERROR] File not found: " + v2Path); System.exit(1); }

        System.out.println("[diff] Comparing metamodels ...");
        System.out.println("  v1 : " + f1.getAbsolutePath());
        System.out.println("  v2 : " + f2.getAbsolutePath());

        
        long sizeDiff = Math.abs(f2.length() - f1.length());
        Map<String, Object> features = estimateFeatures(f1, f2, sizeDiff);

        System.out.println("[diff] Estimated features: " + features);
        System.out.println("[diff] Calling prediction API ...");

        Map<String, Object> result = client.postPredict(features);

        if (result.containsKey("error")) {
            System.err.println("[ERROR] " + result.get("error")); System.exit(1); }

        System.out.println("=" .repeat(50));
        System.out.println("  DIFF + PREDICTION RESULT");
        System.out.println("=" .repeat(50));
        System.out.printf ("  v1 size    : %d bytes%n", f1.length());
        System.out.printf ("  v2 size    : %d bytes%n", f2.length());
        System.out.printf ("  Size diff  : %d bytes%n", sizeDiff);
        System.out.printf ("  Prediction : %s%n", result.get("prediction"));
        System.out.printf ("  Confidence : %s%%%n",
            result.getOrDefault("confidence_pct", "N/A"));
        System.out.println("=" .repeat(50));
    }

    private Map<String, Object> estimateFeatures(File f1, File f2, long sizeDiff) {
        Map<String, Object> m = new HashMap<>();
        int estimated = (int) Math.min(sizeDiff / 100, 5);
        if (f2.length() > f1.length()) {
            m.put("nb_added_classes",    Math.max(1, estimated));
            m.put("nb_added_attributes", estimated);
        } else if (f2.length() < f1.length()) {
            m.put("nb_removed_classes",    Math.max(1, estimated));
            m.put("nb_removed_attributes", estimated);
        } else {
            m.put("nb_type_changes", 1);
        }
        m.put("nb_removed_classes",     m.getOrDefault("nb_removed_classes",    0));
        m.put("nb_added_attributes",    m.getOrDefault("nb_added_attributes",   0));
        m.put("nb_removed_attributes",  m.getOrDefault("nb_removed_attributes", 0));
        m.put("nb_type_changes",        m.getOrDefault("nb_type_changes",       0));
        m.put("nb_added_references",    0);
        m.put("nb_removed_references",  0);
        m.put("nb_multiplicity_changes",0);
        m.put("nb_containment_changes", 0);
        m.put("nb_abstract_changes",    0);
        m.put("nb_supertype_changes",   0);
        m.put("nsuri_changed",          0);
        return m;
    }
}