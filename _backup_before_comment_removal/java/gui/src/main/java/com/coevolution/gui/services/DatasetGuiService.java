package com.coevolution.gui.services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * DatasetGuiService
 * Reads dataset statistics for GUI display.
 * Semaine 8 - GUI
 */
public class DatasetGuiService {

    private final String datasetDir;

    public DatasetGuiService() {
        String root = System.getProperty("user.dir");
        this.datasetDir = root + File.separator + ".."
                        + File.separator + "dataset"
                        + File.separator + "ml";
    }

    public Map<String, Object> getDatasetInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        String[] files = {"features.csv","train.csv","val.csv","test.csv"};
        int total = 0;

        for (String f : files) {
            Path p = Paths.get(datasetDir, f);
            if (Files.exists(p)) {
                int rows = countRows(p.toString()) - 1; // minus header
                info.put(f.replace(".csv","_rows"), Math.max(0, rows));
                if (f.equals("features.csv")) total = Math.max(0, rows);
            } else {
                info.put(f.replace(".csv","_rows"), "N/A");
            }
        }
        info.put("total_rows",  total);
        info.put("dataset_dir", datasetDir);
        info.put("train_rows",  info.getOrDefault("train_rows", "N/A"));
        info.put("val_rows",    info.getOrDefault("val_rows",   "N/A"));
        info.put("test_rows",   info.getOrDefault("test_rows",  "N/A"));

        // read label distribution from features.csv
        Path fp = Paths.get(datasetDir, "features.csv");
        if (Files.exists(fp)) {
            Map<String, Integer> dist = getLabelDistribution(fp.toString());
            info.put("label_distribution", dist.toString());
        }
        return info;
    }

    private int countRows(String path) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path),
                                      StandardCharsets.UTF_8))) {
            int count = 0;
            while (br.readLine() != null) count++;
            return count;
        } catch (IOException e) { return 0; }
    }

    private Map<String, Integer> getLabelDistribution(String path) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path),
                                      StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return dist;
            String[] cols = header.split(",");
            int labelIdx  = -1;
            for (int i = 0; i < cols.length; i++) {
                if (cols[i].trim().equalsIgnoreCase("label")) {
                    labelIdx = i; break; }
            }
            if (labelIdx < 0) return dist;
            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                if (labelIdx < vals.length) {
                    String lbl = vals[labelIdx].trim();
                    dist.merge(lbl, 1, Integer::sum);
                }
            }
        } catch (IOException e) { /* ignore */ }
        return dist;
    }
}