package com.coevolution.gui.services;

import com.coevolution.predictor.FlaskApiClient;
import com.coevolution.migrator.*;
import com.coevolution.migrator.TransformationGenerator.MigrationPlan;
import com.coevolution.migrator.ATLTransformationRunner.MigrationResult;

import java.io.File;
import java.util.*;

/**
 * MigrationGuiService
 * Orchestrates migration from GUI.
 */
public class MigrationGuiService {

    private final FlaskApiClient client;
    private final TransformationGenerator generator;
    private final ATLTransformationRunner runner;
    private final MigrationValidator validator;

    public MigrationGuiService(String apiUrl) {

        this.client = new FlaskApiClient(apiUrl);

        // 🔹 Trouver la racine du projet (où se trouve pom.xml)
        File projectRoot = new File(System.getProperty("user.dir"));
        while (!new File(projectRoot, "pom.xml").exists()) {
            projectRoot = projectRoot.getParentFile();
        }

        File atlDir = new File(projectRoot, "data/transformations");
        File outDir = new File(projectRoot, "data/migrations-output");

        atlDir.mkdirs();
        outDir.mkdirs();

        this.generator = new TransformationGenerator(atlDir.getAbsolutePath());
        this.runner = new ATLTransformationRunner(outDir.getAbsolutePath());
        this.validator = new MigrationValidator();

        System.out.println("[MigrationGuiService]");
        System.out.println("ATL DIR  = " + atlDir.getAbsolutePath());
        System.out.println("OUT DIR  = " + outDir.getAbsolutePath());
    }

    /**
     * Migration principale appelée par le GUI
     */
    public Map<String, Object> migrate(
            String sourceModelPath,
            String targetModelPath,
            Map<String, Integer> features,
            List<String> deltas) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {

            // --------------------------------------------------
            // 1. AI prediction (Flask)
            // --------------------------------------------------

            Map<String, Object> payload = new HashMap<>(features);

            int total = features.values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            payload.put("total_changes", total);

            Map<String, Object> pred = client.postPredict(payload);

            String label = String.valueOf(
                    pred.getOrDefault("prediction", "MIXED"));

            double conf = pred.containsKey("confidence")
                    ? ((Number) pred.get("confidence")).doubleValue()
                    : 0.0;

            System.out.println("[Migration] Prediction = " + label);
            System.out.println("[Migration] Confidence = " + conf);

            // --------------------------------------------------
            // 2. Generate ATL plan
            // --------------------------------------------------

            MigrationPlan plan = generator.generatePlan(
                    sourceModelPath,
                    targetModelPath,
                    label,
                    conf,
                    deltas
            );

            System.out.println("[Migration] ATL FILE = " + plan.getAtlPath());

            // --------------------------------------------------
            // 3. Run ATL migration
            // --------------------------------------------------

            MigrationResult mr = runner.run(plan);

            // --------------------------------------------------
            // 4. Validate result
            // --------------------------------------------------

            MigrationValidator.ValidationResult vr =
                    validator.validate(mr);

            // --------------------------------------------------
            // 5. Build result map
            // --------------------------------------------------

            result.put("prediction", label);
            result.put("confidence", String.format("%.2f%%", conf * 100));
            result.put("atl_file", plan.getAtlFile());

            result.put("output",
                    mr.getOutputPath() != null
                            ? new File(mr.getOutputPath()).getName()
                            : "N/A");

            result.put("success", mr.isSuccess());
            result.put("valid", vr.isValid());
            result.put("duration_ms", mr.getDurationMs());
            result.put("warnings", vr.getWarnings());
            result.put("deltas_used", deltas.size());

        } catch (Exception e) {

            e.printStackTrace();
            result.put("error", e.getMessage());

        }

        return result;
    }
}