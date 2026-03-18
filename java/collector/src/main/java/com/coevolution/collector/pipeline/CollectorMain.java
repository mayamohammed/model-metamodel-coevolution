package com.coevolution.collector.pipeline;

import com.coevolution.collector.git.GitRepositoryManager;

import java.io.File;

/**
 * CollectorMain - point d entree principal du module collector.
 *
 * Usage :
 *   java -jar collector.jar <repoPath> [outputDir] [maxPairs]
 *
 * Arguments :
 *   repoPath  : chemin absolu vers le depot Git local (obligatoire)
 *   outputDir : dossier de sortie du dataset (optionnel, defaut: dataset)
 *   maxPairs  : nombre max de paires a collecter (optionnel, defaut: 50)
 *
 * Exemples :
 *   java -jar collector.jar C:\repos\myrepo
 *   java -jar collector.jar C:\repos\myrepo dataset 100
 */
public class CollectorMain {

    public static void main(String[] args) throws Exception {

        // ── Lire les arguments ────────────────────────────────────────────────
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String repoPath  = args[0];
        String outputDir = args.length > 1 ? args[1] : "dataset";
        int    maxPairs  = args.length > 2 ? parseIntSafe(args[2], 100) : 100;

        // ── Afficher la configuration ─────────────────────────────────────────
        System.out.println("=================================================");
        System.out.println("[CollectorMain] Demarrage du Collector");
        System.out.println("[CollectorMain] Repo     : " + repoPath);
        System.out.println("[CollectorMain] Output   : " + outputDir);
        System.out.println("[CollectorMain] MaxPairs : " + maxPairs);
        System.out.println("=================================================");

        // ── Verifier que le chemin du repo existe ─────────────────────────────
        File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory()) {
            System.err.println("[CollectorMain] ERREUR : le chemin du repo n existe pas : " + repoPath);
            System.exit(1);
        }

        // ── Ouvrir le depot Git ───────────────────────────────────────────────
        GitRepositoryManager gitManager = new GitRepositoryManager();
        try {
            gitManager.openLocal(repoPath);
            System.out.println("[CollectorMain] Depot Git ouvert avec succes.");

            // ── Lancer le pipeline ────────────────────────────────────────────
            File outDir = new File(outputDir);
            CollectionPipeline pipeline = new CollectionPipeline(gitManager, outDir);
            pipeline.setMaxPairs(maxPairs);
            pipeline.run();

            // ── Afficher le resume final ──────────────────────────────────────
            System.out.println("\n[CollectorMain] Resume :");
            System.out.println("  Paires collectees : " + pipeline.getPairsCollected());
            System.out.println("  Paires identiques : " + pipeline.getPairsIdentical());
            System.out.println("  Paires invalides  : " + pipeline.getPairsInvalid());
            System.out.println("  Paires skippees   : " + pipeline.getPairsSkipped());
            System.out.println("[CollectorMain] Dataset disponible dans : "
                    + outDir.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("[CollectorMain] ERREUR fatale : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            gitManager.close();
        }
    }

    /** Affiche l aide sur l utilisation. */
    private static void printUsage() {
        System.out.println("Usage : CollectorMain <repoPath> [outputDir] [maxPairs]");
        System.out.println("");
        System.out.println("  repoPath  : chemin absolu vers le depot Git local (obligatoire)");
        System.out.println("  outputDir : dossier de sortie du dataset (defaut: dataset)");
        System.out.println("  maxPairs  : nombre max de paires a collecter (defaut: 50)");
        System.out.println("");
        System.out.println("Exemples :");
        System.out.println("  CollectorMain C:\\repos\\myrepo");
        System.out.println("  CollectorMain C:\\repos\\myrepo dataset 100");
    }

    /** Parse un entier avec une valeur par defaut si erreur. */
    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("[CollectorMain] Valeur maxPairs invalide, utilisation defaut: "
                    + defaultValue);
            return defaultValue;
        }
    }
}