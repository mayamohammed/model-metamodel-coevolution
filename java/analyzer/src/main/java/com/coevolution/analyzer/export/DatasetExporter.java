package com.coevolution.analyzer.export;

import com.coevolution.analyzer.features.FeatureExtractor;
import com.coevolution.analyzer.features.FeatureVector;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DatasetExporter {

    private final File             datasetDir;
    private final File             outputDir;
    private final FeatureExtractor extractor;

    private int totalPairs    = 0;
    private int totalExported = 0;
    private int totalFailed   = 0;
    private int totalUnknown  = 0;

    public DatasetExporter(File datasetDir, File outputDir) {
        this.datasetDir = datasetDir;
        this.outputDir  = outputDir;
        this.extractor  = new FeatureExtractor();
    }

    public void export() throws Exception {
        outputDir.mkdirs();

        List<FeatureVector> vectors = new ArrayList<>();

        
        File[] pairDirs = datasetDir.listFiles(File::isDirectory);
        if (pairDirs == null || pairDirs.length == 0) {
            System.err.println("   ⚠️  Aucun dossier trouvé dans : "
                    + datasetDir.getAbsolutePath());
            return;
        }

        System.out.println("[DatasetExporter] " + pairDirs.length
                + " paires trouvées dans : " + datasetDir.getName());

        for (File pairDir : pairDirs) {
            if (!pairDir.isDirectory()) continue;

            
            File v1 = new File(pairDir, "v1.ecore");
            File v2 = new File(pairDir, "v2.ecore");
            if (!v1.exists() || !v2.exists()) {
                totalFailed++;
                continue;
            }

            try {
                FeatureVector fv = extractor.extract(pairDir);
                vectors.add(fv);
                if ("UNKNOWN".equals(fv.getLabel())) totalUnknown++;
                totalPairs++;
                totalExported++;
            } catch (Exception e) {
                System.err.println("  [SKIP] " + pairDir.getName()
                        + " : " + e.getMessage());
                totalFailed++;
            }
        }

        
        File csvFile = new File(outputDir, "features.csv");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {
            pw.println(FeatureVector.csvHeader());
            for (FeatureVector fv : vectors) {
                pw.println(fv.toCsvRow());
            }
        }

        
        Map<String, Integer> labelCount = new LinkedHashMap<>();
        for (FeatureVector fv : vectors)
            labelCount.merge(fv.getLabel(), 1, Integer::sum);

        printSummary(csvFile, labelCount);
    }

    private void printSummary(File csvFile, Map<String, Integer> labelCount) {
        System.out.println("\n=================================================");
        System.out.println("  EXPORT FEATURES TERMINE");
        System.out.println("=================================================");
        System.out.println("  Total paires traitees : " + totalPairs);
        System.out.println("  Echecs                : " + totalFailed);
        System.out.println("  Labels UNKNOWN        : " + totalUnknown);
        System.out.println("  CSV genere            : " + csvFile.getAbsolutePath());
        System.out.println("-------------------------------------------------");
        System.out.println("  Distribution des labels :");
        labelCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("    %-40s : %d%n",
                        e.getKey(), e.getValue()));
        System.out.println("=================================================");
    }

    public int getTotalPairs()    { return totalPairs;    }
    public int getTotalExported() { return totalExported; }
    public int getTotalFailed()   { return totalFailed;   }
    public int getTotalUnknown()  { return totalUnknown;  }
}
