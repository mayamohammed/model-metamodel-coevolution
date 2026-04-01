package com.coevolution.migrator;

import com.coevolution.migrator.ATLTransformationRunner.MigrationResult;
import com.coevolution.migrator.MigrationValidator.ValidationResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MigrationReportExporter
 *
 * Exports migration results to JSON and CSV reports.
 *
 * Semaine 7 - Migration ATL
 */
public class MigrationReportExporter {

    private final String reportsDir;

    public MigrationReportExporter(String reportsDir) {
        this.reportsDir = reportsDir;
        new File(reportsDir).mkdirs();
    }

    // ── Export full migration report ──────────────────────────────
    public String exportReport(List<MigrationResult>    results,
                                List<ValidationResult>   validations) {
        String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path = reportsDir + File.separator
            + "migration_report_" + ts + ".json";

        // compute stats
        long total   = results.size();
        long success = results.stream().filter(MigrationResult::isSuccess).count();
        long valid   = validations.stream().filter(ValidationResult::isValid).count();
        long failed  = total - success;

        // label distribution
        Map<String, Integer> dist = new LinkedHashMap<>();
        for (MigrationResult r : results) {
            if (r.getPlan() != null) {
                String lbl = r.getPlan().getPredictionLabel();
                dist.merge(lbl, 1, Integer::sum);
            }
        }

        // build JSON
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"report_type\": \"migration_s7\",\n");
        sb.append("  \"date\": \"").append(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
            .append("\",\n");
        sb.append("  \"semaine\": 7,\n");
        sb.append("  \"summary\": {\n");
        sb.append("    \"total\":   ").append(total).append(",\n");
        sb.append("    \"success\": ").append(success).append(",\n");
        sb.append("    \"failed\":  ").append(failed).append(",\n");
        sb.append("    \"valid\":   ").append(valid).append(",\n");
        sb.append("    \"success_rate_pct\": ")
            .append(total > 0 ? String.format("%.2f", success * 100.0 / total) : "0")
            .append(",\n");
        sb.append("    \"validation_rate_pct\": ")
            .append(total > 0 ? String.format("%.2f", valid * 100.0 / total) : "0")
            .append("\n  },\n");

        // label distribution
        sb.append("  \"label_distribution\": {\n");
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(dist.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            sb.append("    \"").append(entries.get(i).getKey())
              .append("\": ").append(entries.get(i).getValue());
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  },\n");

        // details
        sb.append("  \"details\": [\n");
        for (int i = 0; i < results.size(); i++) {
            MigrationResult  r = results.get(i);
            ValidationResult v = i < validations.size() ? validations.get(i) : null;
            sb.append("    {\n");
            sb.append("      \"index\": ").append(i).append(",\n");
            if (r.getPlan() != null) {
                sb.append("      \"source\":     \"")
                  .append(new File(r.getPlan().getSourceModel()).getName())
                  .append("\",\n");
                sb.append("      \"atl_file\":   \"")
                  .append(r.getPlan().getAtlFile()).append("\",\n");
                sb.append("      \"label\":      \"")
                  .append(r.getPlan().getPredictionLabel()).append("\",\n");
                sb.append("      \"confidence\": ")
                  .append(String.format("%.4f", r.getPlan().getConfidence()))
                  .append(",\n");
            }
            sb.append("      \"success\":    ").append(r.isSuccess()).append(",\n");
            sb.append("      \"duration_ms\":").append(r.getDurationMs()).append(",\n");
            sb.append("      \"lines_in\":   ").append(r.getLinesIn()).append(",\n");
            sb.append("      \"lines_out\":  ").append(r.getLinesOut()).append(",\n");
            sb.append("      \"valid\":      ")
              .append(v != null && v.isValid()).append(",\n");
            sb.append("      \"warnings\":   ")
              .append(v != null ? v.getWarnings().size() : 0).append("\n");
            sb.append("    }");
            if (i < results.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8))) {
            pw.print(sb.toString());
        } catch (IOException e) {
            System.err.println("[EXP] Error writing report: " + e.getMessage());
        }

        System.out.printf("[EXP] Report saved: %s%n", path);
        System.out.printf("[EXP] Summary: %d/%d success | %d/%d valid%n",
            success, total, valid, total);
        return path;
    }

    // ── Export CSV summary ────────────────────────────────────────
    public String exportCsv(List<MigrationResult> results) {
        String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path = reportsDir + File.separator
            + "migration_summary_" + ts + ".csv";

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(path), StandardCharsets.UTF_8))) {
            pw.println("index,source,label,atl_file,confidence,success,duration_ms,lines_in,lines_out");
            for (int i = 0; i < results.size(); i++) {
                MigrationResult r = results.get(i);
                String src   = r.getPlan() != null
                    ? new File(r.getPlan().getSourceModel()).getName() : "";
                String lbl   = r.getPlan() != null ? r.getPlan().getPredictionLabel() : "";
                String atl   = r.getPlan() != null ? r.getPlan().getAtlFile() : "";
                double conf  = r.getPlan() != null ? r.getPlan().getConfidence() : 0;
                pw.printf("%d,%s,%s,%s,%.4f,%b,%d,%d,%d%n",
                    i, src, lbl, atl, conf,
                    r.isSuccess(), r.getDurationMs(),
                    r.getLinesIn(), r.getLinesOut());
            }
        } catch (IOException e) {
            System.err.println("[EXP] Error writing CSV: " + e.getMessage());
        }
        System.out.printf("[EXP] CSV saved: %s%n", path);
        return path;
    }

    public String getReportsDir() { return reportsDir; }
}