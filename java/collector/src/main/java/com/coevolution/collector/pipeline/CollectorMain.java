package com.coevolution.collector.pipeline;

import com.coevolution.collector.git.GitRepositoryManager;
import java.io.File;
import java.io.IOException;

/**
 * CollectorMain - TOTAL Ecore + collecte
 */
public class CollectorMain {

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String repoPath  = args[0];
        String outputDir = args.length > 1 ? args[1] : "dataset";
        int    maxPairs  = args.length > 2 ? parseIntSafe(args[2], 100) : 100;

        System.out.println("=================================================");
        System.out.println("[CollectorMain] Demarrage du Collector");
        System.out.println("[CollectorMain] Repo     : " + repoPath);
        System.out.println("[CollectorMain] Output   : " + outputDir);
        System.out.println("[CollectorMain] MaxPairs : " + maxPairs);
        System.out.println("=================================================");

        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            System.err.println("[CollectorMain] ERREUR : chemin invalide : " + repoPath);
            System.exit(1);
        }

        // 👇 TOTAL ECORES AVANT
        GitRepositoryManager gitManager = new GitRepositoryManager();
        gitManager.scanAllRepos(repoPath);
        System.out.println("📊 TOTAL AVANT: " + gitManager.getTotalEcoreFiles() + " Ecore");
        
        try {
            // Pipeline existant
            File outDir = new File(outputDir);
            CollectionPipeline pipeline = new CollectionPipeline(gitManager, outDir);
            pipeline.setMaxPairs(maxPairs);
            pipeline.run();

            // Résumé FINAL
            System.out.println("\n🎯 RÉSULTAT FINAL:");
            System.out.println("   Ecore scannés : " + gitManager.getTotalEcoreFiles());
            System.out.println("   Paires OK     : " + pipeline.getPairsCollected());
            System.out.println("   Identiques    : " + pipeline.getPairsIdentical());
            System.out.println("   Invalides     : " + pipeline.getPairsInvalid());
            System.out.println("   Skip          : " + pipeline.getPairsSkipped());
            System.out.println("   Dataset       : " + outDir.getAbsolutePath());
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("[CollectorMain] ERREUR : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            gitManager.close();
        }
    }

    private static void printUsage() {
        System.out.println("Usage : CollectorMain <repoPath> [outputDir] [maxPairs]");
        System.out.println("Ex: CollectorMain ../../data dataset 500");
    }

    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
