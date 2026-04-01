package com.coevolution.collector.pipeline;

import com.coevolution.collector.git.GitRepositoryManager;
import java.io.File;

public class CollectorMain {

    private static final String[][] SOURCES = {
        { "../../data/repogit",   "dataset_repos",   "500" },
        { "../../data/domains",   "dataset_domains", "500" },
        { "../../data/synthetic", "dataset_synth",   "500" }
    };

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("  COLLECTOR MULTI-SOURCES");
        System.out.println("=================================================");

        int totalPaires = 0;

        for (String[] source : SOURCES) {
            String repoPath  = source[0];
            String outputDir = source[1];
            int    maxPairs  = Integer.parseInt(source[2]);

            File repoDir = new File(repoPath);

            System.out.println("\n[SOURCE] " + repoDir.getAbsolutePath());
            System.out.println("   Output  : " + outputDir);
            System.out.println("   Max     : " + maxPairs);
            System.out.println("-------------------------------------------------");

            if (!repoDir.exists() || !repoDir.isDirectory()) {
                System.err.println("   [SKIP] dossier introuvable");
                continue;
            }

            File[] subDirs = repoDir.listFiles(File::isDirectory);
            if (subDirs == null || subDirs.length == 0) {
                System.out.println("   [SKIP] dossier vide : " + repoDir.getAbsolutePath());
                continue;
            }

            System.out.println("   Sous-dossiers : " + subDirs.length);

            GitRepositoryManager gitManager = new GitRepositoryManager();
            try {
                gitManager.scanAllRepos(repoPath);
                System.out.println("   Ecore trouves : " + gitManager.getTotalEcoreFiles());

                if (gitManager.getTotalEcoreFiles() == 0) {
                    System.out.println("   [SKIP] Aucun .ecore trouve");
                    continue;
                }

                File outDir = new File(outputDir);
                CollectionPipeline pipeline =
                        new CollectionPipeline(gitManager, outDir);
                pipeline.setMaxPairs(maxPairs);
                pipeline.run();

                int collected = pipeline.getPairsCollected();
                totalPaires  += collected;

                System.out.println("   [OK] Paires    : " + collected);
                System.out.println("   Identiques     : " + pipeline.getPairsIdentical());
                System.out.println("   Invalides      : " + pipeline.getPairsInvalid());
                System.out.println("   Skip           : " + pipeline.getPairsSkipped());
                System.out.println("   Dataset        : " + outDir.getAbsolutePath());

            } catch (Exception e) {
                System.err.println("   [ERREUR] " + e.getMessage());
                e.printStackTrace();
            } finally {
                gitManager.close();
            }
        }

        System.out.println("\n=================================================");
        System.out.println("  RESULTAT FINAL TOUTES SOURCES");
        System.out.println("=================================================");
        System.out.println("  TOTAL PAIRES COLLECTEES : " + totalPaires);
        System.out.println("=================================================");
    }
}
