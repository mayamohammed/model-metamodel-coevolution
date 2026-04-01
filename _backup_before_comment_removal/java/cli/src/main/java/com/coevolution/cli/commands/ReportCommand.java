package com.coevolution.cli.commands;

import com.coevolution.predictor.PredictionService;
import com.coevolution.predictor.PredictionService.FeatureVector;
import com.coevolution.predictor.PredictionService.PredictionResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ReportCommand
 *
 * CLI command: report --input <features.csv> --output <dir>
 * Reads a CSV of features, calls /predict/batch,
 * and writes a prediction report.
 *
 * Semaine 6 - API + CLI
 */
public class ReportCommand {

    private final PredictionService service;

    public ReportCommand(String apiUrl) {
        this.service = new PredictionService(apiUrl);
    }

    public void execute(String[] args) {
        String inputPath  = null;
        String outputDir  = null;

        for (int i = 1; i < args.length - 1; i++) {
            switch (args[i].toLowerCase()) {
                case "--input":  inputPath = args[i + 1]; i++; break;
                case "--output": outputDir  = args[i + 1]; i++; break;
            }
        }

        if (inputPath == null) {
            System.err.println("[ERROR] Usage: report --input <features.csv> --output <dir>");
            System.exit(1);
        }
        if (outputDir == null) outputDir = ".";

        System.out.println("[report] Input  : " + inputPath);
        System.out.println("[report] Output : " + outputDir);

        // Load CSV
        List<FeatureVector> vectors = loadCsv(inputPath);
        System.out.println("[report] Loaded " + vectors.size() + " rows");

        if (vectors.isEmpty()) {
            System.err.println("[ERROR] No rows found in CSV"); System.exit(1); }

        // Predict batch
        System.out.println("[report] Calling API batch prediction ...");
        if (!service.isApiHealthy()) {
            System.err.println("[ERROR] API not reachable"); System.exit(1); }

        List<PredictionResult> results = service.predictBatch(vectors);

        // Write report
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outPath   = outputDir + File.separator + "prediction_report_" + timestamp + ".csv";
        writeReport(results, outPath);

        // Print summary
        long errors  = results.stream().filter(PredictionResult::isError).count();
        long success = results.size() - errors;
        Map<String, Long> dist = new LinkedHashMap<>();
        for (PredictionResult r : results) {
            if (!r.isError()) dist.merge(r.getPrediction(), 1L, Long::sum);
        }

        System.out.println("=" .repeat(55));
        System.out.println("  REPORT SUMMARY");
        System.out.println("=" .repeat(55));
        System.out.printf ("  Total rows  : %d%n", results.size());
        System.out.printf ("  Success     : %d%n", success);
        System.out.printf ("  Errors      : %d%n", errors);
        System.out.println("  Distribution:");
        dist.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(e -> System.out.printf(
                "    %-40s : %d%n", e.getKey(), e.getValue()));
        System.out.println("  Report saved: " + outPath);
        System.out.println("=" .repeat(55));
    }

    private List<FeatureVector> loadCsv(String path) {
        List<FeatureVector> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path),
                                      StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return list;
            String[] headers = headerLine.split(",");

            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                FeatureVector fv = new FeatureVector();
                for (int i = 0; i < headers.length && i < vals.length; i++) {
                    String h = headers[i].trim().toLowerCase();
                    int v = 0;
                    try { v = (int) Double.parseDouble(vals[i].trim()); }
                    catch (NumberFormatException ex) { /* skip */ }
                    switch (h) {
                        case "nb_added_classes":       fv.setNbAddedClasses(v);       break;
                        case "nb_removed_classes":     fv.setNbRemovedClasses(v);     break;
                        case "nb_added_attributes":    fv.setNbAddedAttributes(v);    break;
                        case "nb_removed_attributes":  fv.setNbRemovedAttributes(v);  break;
                        case "nb_type_changes":        fv.setNbTypeChanges(v);        break;
                        case "nb_added_references":    fv.setNbAddedReferences(v);    break;
                        case "nb_removed_references":  fv.setNbRemovedReferences(v);  break;
                        case "nb_multiplicity_changes":fv.setNbMultiplicityChanges(v);break;
                        case "nb_containment_changes": fv.setNbContainmentChanges(v); break;
                        case "nb_abstract_changes":    fv.setNbAbstractChanges(v);    break;
                        case "nb_supertype_changes":   fv.setNbSupertypeChanges(v);   break;
                        case "nsuri_changed":           fv.setNsUriChanged(v);         break;
                    }
                }
                list.add(fv);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Reading CSV: " + e.getMessage());
        }
        return list;
    }

    private void writeReport(List<PredictionResult> results, String outPath) {
        new File(outPath).getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(outPath), StandardCharsets.UTF_8))) {
            pw.println("index,prediction,confidence_pct,error");
            for (int i = 0; i < results.size(); i++) {
                PredictionResult r = results.get(i);
                if (r.isError()) {
                    pw.printf("%d,,,%s%n", i, r.getError());
                } else {
                    pw.printf("%d,%s,%.2f,%n",
                        i, r.getPrediction(), r.getConfidencePct());
                }
            }
            System.out.println("[OK] Report written: " + outPath);
        } catch (IOException e) {
            System.err.println("[ERROR] Writing report: " + e.getMessage());
        }
    }
}