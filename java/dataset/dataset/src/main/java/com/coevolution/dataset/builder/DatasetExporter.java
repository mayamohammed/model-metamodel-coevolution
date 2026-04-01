package com.coevolution.dataset.builder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DatasetExporter {

    private final String outputDir;
    private final double trainRatio;
    private final double valRatio;
    private final double testRatio;
    private final Random random;

    
    public DatasetExporter(String outputDir,
                           double trainRatio,
                           double valRatio,
                           double testRatio) {
        if (Math.abs(trainRatio + valRatio + testRatio - 1.0) > 1e-6)
            throw new IllegalArgumentException(
                "trainRatio + valRatio + testRatio must equal 1.0");
        this.outputDir  = outputDir;
        this.trainRatio = trainRatio;
        this.valRatio   = valRatio;
        this.testRatio  = testRatio;
        this.random     = new Random(42);
    }

    public DatasetExporter(String outputDir) {
        this(outputDir, 0.70, 0.15, 0.15);
    }

    
    
    public void export(DatasetBuilder builder) throws IOException {
        List<DatasetBuilder.DatasetEntry> entries =
            new ArrayList<>(builder.getEntries());
        Collections.shuffle(entries, random);

        int n         = entries.size();
        int trainEnd  = (int) (n * trainRatio);
        int valEnd    = trainEnd + (int) (n * valRatio);

        List<DatasetBuilder.DatasetEntry> trainSet = entries.subList(0, trainEnd);
        List<DatasetBuilder.DatasetEntry> valSet   = entries.subList(trainEnd, valEnd);
        List<DatasetBuilder.DatasetEntry> testSet  = entries.subList(valEnd, n);

        writeFile(builder, trainSet, outputDir + "/train.csv");
        writeFile(builder, valSet,   outputDir + "/val.csv");
        writeFile(builder, testSet,  outputDir + "/test.csv");
        writeFile(builder, entries,  outputDir + "/features.csv");

        System.out.printf(
            "Dataset exporte : train=%d | val=%d | test=%d | total=%d%n",
            trainSet.size(), valSet.size(), testSet.size(), n);
    }

    
    private void writeFile(DatasetBuilder builder,
                           List<DatasetBuilder.DatasetEntry> entries,
                           String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            bw.write(builder.getCsvHeader());
            bw.newLine();
            for (DatasetBuilder.DatasetEntry e : entries) {
                bw.write(builder.toCsvLine(e));
                bw.newLine();
            }
        }
        System.out.println("Fichier ecrit : " + filePath
            + " (" + entries.size() + " lignes)");
    }

    
    public void exportFull(DatasetBuilder builder,
                           String fileName) throws IOException {
        String filePath = outputDir + "/" + fileName;
        writeFile(builder, builder.getEntries(), filePath);
    }

    
    public String getOutputDir()  { return outputDir;  }
    public double getTrainRatio() { return trainRatio; }
    public double getValRatio()   { return valRatio;   }
    public double getTestRatio()  { return testRatio;  }
}