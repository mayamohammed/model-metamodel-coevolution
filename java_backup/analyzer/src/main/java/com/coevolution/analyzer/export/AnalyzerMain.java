
package com.coevolution.analyzer.export;

import java.io.File;

/**
 * AnalyzerMain — point d entree du module analyzer.
 *
 * Arguments :
 *   args[0] : chemin du dataset (contient pairs/ et augmented/)
 *   args[1] : chemin output ML (dataset/ml/)
 */
public class AnalyzerMain {

    public static void main(String[] args) throws Exception {
        String datasetPath = args.length > 0 ? args[0]
            : "C:\\Users\\maya mohammed\\eclipse-workspace"
              + "\\model-metamodel-coevolution3\\dataset";
        String outputPath  = args.length > 1 ? args[1]
            : "C:\\Users\\maya mohammed\\eclipse-workspace"
              + "\\model-metamodel-coevolution3\\dataset\\ml";

        System.out.println("=================================================");
        System.out.println("  ANALYZER — EXTRACTION FEATURES ML");
        System.out.println("  Semaine 3b");
        System.out.println("=================================================");
        System.out.println("  Dataset : " + datasetPath);
        System.out.println("  Output  : " + outputPath);
        System.out.println("=================================================");

        File datasetDir = new File(datasetPath);
        File outputDir  = new File(outputPath);

        if (!datasetDir.exists()) {
            System.err.println("ERREUR : dataset introuvable -> " + datasetPath);
            System.exit(1);
        }

        DatasetExporter exporter = new DatasetExporter(datasetDir, outputDir);
        exporter.export();

        System.out.println("\n✅ features.csv pret pour Python ML !");
        System.out.println("   -> " + outputPath + "\\features.csv");
    }
}