package com.coevolution.analyzer.pipeline;

import com.coevolution.analyzer.export.DatasetExporter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnalyzerMain {

    private static final String[][] SOURCES = {
        { "../collector/dataset_repos/pairs",   "ml_repos"   },
        { "../collector/dataset_domains/pairs", "ml_domains" },
        { "../collector/dataset_synth/pairs",   "ml_synth"   },
        { "../augmentation/augmented_repos",    "ml_augmented_repos"   },
        { "../augmentation/augmented_domains",  "ml_augmented_domains" },
        { "../augmentation/augmented_synth",    "ml_augmented_synth"   }
    };

    private static final String BASE_OUTPUT = "../../data/ml_final";
    private static final String MERGED_CSV  = BASE_OUTPUT + "/features_all.csv";
    private static final String TRAIN_CSV   = BASE_OUTPUT + "/train.csv";
    private static final String VAL_CSV     = BASE_OUTPUT + "/val.csv";
    private static final double TRAIN_RATIO = 0.8;

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("  ANALYZER - EXTRACTION FEATURES ML");
        System.out.println("=================================================");

        int totalPairs    = 0;
        int totalExported = 0;

        for (String[] source : SOURCES) {
            String datasetPath = source[0];
            String outputPath  = source[1];

            File datasetDir = new File(datasetPath);
            File outputDir  = new File(outputPath);

            System.out.println("\n[SOURCE] " + datasetDir.getAbsolutePath());
            System.out.println("   Output : " + outputDir.getAbsolutePath());
            System.out.println("-------------------------------------------------");

            if (!datasetDir.exists() || !datasetDir.isDirectory()) {
                System.err.println("   [SKIP] dossier introuvable");
                continue;
            }

            try {
                DatasetExporter exporter = new DatasetExporter(datasetDir, outputDir);
                exporter.export();

                totalPairs    += exporter.getTotalPairs();
                totalExported += exporter.getTotalExported();

                System.out.println("   [OK] Paires traitees : " + exporter.getTotalPairs());
                System.out.println("   [OK] CSV             : "
                        + outputDir.getAbsolutePath() + "/features.csv");

            } catch (Exception e) {
                System.err.println("   [ERREUR] " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=================================================");
        System.out.println("  RESULTAT FINAL");
        System.out.println("=================================================");
        System.out.println("  Paires analysees  : " + totalPairs);
        System.out.println("  Features exportes : " + totalExported);
        System.out.println("=================================================");

        String[] allOutputs = {
            "ml_repos", "ml_domains", "ml_synth",
            "ml_augmented_repos", "ml_augmented_domains", "ml_augmented_synth"
        };
        mergeAllCsv(allOutputs, MERGED_CSV);
        splitTrainVal(MERGED_CSV, TRAIN_CSV, VAL_CSV, TRAIN_RATIO);
    }

    private static void mergeAllCsv(String[] outputDirs,
                                     String mergedPath) throws Exception {
        File mergedFile = new File(mergedPath);
        mergedFile.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
            // ✅ BOM UTF-8
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

                // ✅ Force séparateur virgule pour Excel
                pw.println("sep=,");

                boolean headerWritten = false;
                int totalLines = 0;

                for (String dir : outputDirs) {
                    File csv = new File(dir + "/features.csv");
                    if (!csv.exists()) {
                        System.out.println("[MERGE] SKIP (absent) : " + csv.getPath());
                        continue;
                    }

                    List<String> lines = Files.readAllLines(
                            csv.toPath(), StandardCharsets.UTF_8);
                    if (lines.isEmpty()) continue;

                    if (!headerWritten) {
                        pw.println(lines.get(0));
                        headerWritten = true;
                    }

                    int count = 0;
                    for (int i = 1; i < lines.size(); i++) {
                        if (!lines.get(i).trim().isEmpty()) {
                            pw.println(lines.get(i));
                            count++;
                        }
                    }
                    totalLines += count;
                    System.out.println("[MERGE] " + dir + " -> " + count + " lignes");
                }

                System.out.println("\n=================================================");
                System.out.println("  MERGE TERMINE");
                System.out.println("=================================================");
                System.out.println("  Fichier : " + mergedFile.getAbsolutePath());
                System.out.println("  TOTAL   : " + totalLines + " paires");
                System.out.println("=================================================");
            }
        }
    }

    private static void splitTrainVal(String mergedPath,
                                       String trainPath,
                                       String valPath,
                                       double trainRatio) throws Exception {
        File mergedFile = new File(mergedPath);
        if (!mergedFile.exists()) {
            System.err.println("[SPLIT] Fichier merged introuvable : " + mergedPath);
            return;
        }

        List<String> lines = Files.readAllLines(
                mergedFile.toPath(), StandardCharsets.UTF_8);
        if (lines.size() < 2) return;

        // Sauter sep=, et header (2 premières lignes)
        String header = lines.get(1);
        List<String> data = new ArrayList<>(lines.subList(2, lines.size()));

        Collections.shuffle(data);

        int trainSize = (int) (data.size() * trainRatio);
        List<String> trainData = data.subList(0, trainSize);
        List<String> valData   = data.subList(trainSize, data.size());

        writeCsv(new File(trainPath), header, trainData);
        writeCsv(new File(valPath),   header, valData);

        System.out.println("\n=================================================");
        System.out.println("  SPLIT TRAIN / VAL");
        System.out.println("=================================================");
        System.out.println("  Train : " + trainData.size()
                + " paires -> " + new File(trainPath).getAbsolutePath());
        System.out.println("  Val   : " + valData.size()
                + " paires -> " + new File(valPath).getAbsolutePath());
        System.out.println("=================================================");
    }

    private static void writeCsv(File file,
                                   String header,
                                   List<String> rows) throws Exception {
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // ✅ BOM UTF-8
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                // ✅ Force séparateur virgule pour Excel
                pw.println("sep=,");
                pw.println(header);
                for (String row : rows) {
                    if (!row.trim().isEmpty()) pw.println(row);
                }
            }
        }
    }
}
