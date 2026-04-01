package com.coevolution.gui.services;

import com.coevolution.predictor.PredictionService;
import com.coevolution.predictor.PredictionService.*;
import com.coevolution.migrator.MigrationReportExporter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReportGuiService {

    private final PredictionService     service;
    private final MigrationReportExporter exporter;

    public ReportGuiService(String apiUrl) {
        this.service  = new PredictionService(apiUrl);
        this.exporter = new MigrationReportExporter(
            System.getProperty("user.dir") + File.separator + "reports");
    }

    public String generateReport(String inputCsv, String outputDir) {
        StringBuilder log = new StringBuilder();
        try {
            List<FeatureVector> vectors = loadCsv(inputCsv, log);
            log.append("[INFO] Loaded ").append(vectors.size())
               .append(" rows from ").append(inputCsv).append("\n");

            if (vectors.isEmpty()) {
                log.append("[WARN] No rows found\n");
                return log.toString();
            }

            List<PredictionResult> results = service.predictBatch(vectors);
            long ok   = results.stream().filter(r -> !r.isError()).count();
            long err  = results.size() - ok;

            log.append("[INFO] Predictions: ").append(ok)
               .append(" OK / ").append(err).append(" errors\n");

            
            Map<String, Long> dist = new LinkedHashMap<>();
            for (PredictionResult r : results) {
                if (!r.isError())
                    dist.merge(r.getPrediction(), 1L, Long::sum);
            }
            log.append("\nDistribution:\n");
            dist.entrySet().stream()
                .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> log.append(String.format(
                    "  %-40s : %d%n", e.getKey(), e.getValue())));

            log.append("\n[OK] Report complete\n");

        } catch (Exception e) {
            log.append("[ERROR] ").append(e.getMessage()).append("\n");
        }
        return log.toString();
    }

    private List<FeatureVector> loadCsv(String path, StringBuilder log) {
        List<FeatureVector> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path),
                                      StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return list;
            String[] cols = header.split(",");
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                FeatureVector fv = new FeatureVector();
                for (int i = 0; i < cols.length && i < vals.length; i++) {
                    int v = 0;
                    try { v = (int) Double.parseDouble(vals[i].trim()); }
                    catch (NumberFormatException ex) {  }
                    switch (cols[i].trim().toLowerCase()) {
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
                idx++;
            }
        } catch (IOException e) {
            log.append("[ERROR] Reading CSV: ").append(e.getMessage()).append("\n");
        }
        return list;
    }
}