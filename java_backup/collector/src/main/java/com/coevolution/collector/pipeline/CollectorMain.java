package com.coevolution.collector.pipeline;

import com.coevolution.collector.git.GitRepositoryManager;
import java.io.File;

public class CollectorMain {

    // ✅ Chemins relatifs depuis java/collector/
    private static final String[][] SOURCES = {
        // { inputPath,              outputDir,          maxPairs }
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

            System.out.println("\n📂 SOURCE : " + new File(repoPath).getAbsolutePath());
            System.out.println("   Output  : " + outputDir);
            System.out.println("   Max     : " + maxPairs);
            System.out.println("-------------------------------------------------");

            File repoDir = new File(repoPath);
            if (!repoDir.exists() || !repoDir.isDirectory()) {
                System.err.println("   ⚠️  SKIP : dossier introuvable → "
                        + repoDir.getAbsolutePath());
                continue;
            }

            GitRepositoryManager gitManager = new GitRepositoryManager();
            try {
                gitManager.scanAllRepos(repoPath);
                System.out.println("   Ecore trouvés : "
                        + gitManager.getTotalEcoreFiles());

                File outDir = new File(outputDir);
                CollectionPipeline pipeline =
                        new CollectionPipeline(gitManager, outDir);
                pipeline.setMaxPairs(maxPairs);
                pipeline.run();

                int collected = pipeline.getPairsCollected();
                totalPaires += collected;

                System.out.println("   ✅ Paires OK  : " + collected);
                System.out.println("   Identiques    : " + pipeline.getPairsIdentical());
                System.out.println("   Invalides     : " + pipeline.getPairsInvalid());
                System.out.println("   Skip          : " + pipeline.getPairsSkipped());
                System.out.println("   Dataset       : " + outDir.getAbsolutePath());

            } catch (Exception e) {
                System.err.println("   ❌ ERREUR : " + e.getMessage());
                e.printStackTrace();
            } finally {
                gitManager.close();
            }
        }

        System.out.println("\n=================================================");
        System.out.println("  RÉSULTAT FINAL TOUTES SOURCES");
        System.out.println("=================================================");
        System.out.println("  TOTAL PAIRES COLLECTÉES : " + totalPaires);
        System.out.println("=================================================");
    }
}
