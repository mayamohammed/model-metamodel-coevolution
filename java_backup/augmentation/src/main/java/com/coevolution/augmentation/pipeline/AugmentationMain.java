package com.coevolution.augmentation.pipeline;

import com.coevolution.augmentation.generator.PairGenerator;
import java.io.File;

public class AugmentationMain {

    // ✅ Chemins relatifs depuis java/augmentation/
    private static final String[][] SOURCES = {
        // { inputPath,                          outputDir,          maxMutations }
        { "../collector/dataset_repos",   "augmented_repos",   "40" },
        { "../collector/dataset_domains", "augmented_domains", "40" },
        { "../collector/dataset_synth",   "augmented_synth",   "40" }
    };

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("  AUGMENTATION MULTI-SOURCES");
        System.out.println("=================================================");

        int totalGenerated = 0;
        int totalOriginal  = 0;

        for (String[] source : SOURCES) {
            String inputPath  = source[0];
            String outputDir  = source[1];
            int    maxMut     = Integer.parseInt(source[2]);

            System.out.println("\n📂 SOURCE : " + new File(inputPath).getAbsolutePath());
            System.out.println("   Output  : " + outputDir);
            System.out.println("   Max mut : " + maxMut);
            System.out.println("-------------------------------------------------");

            File inputDir = new File(inputPath);
            if (!inputDir.exists() || !inputDir.isDirectory()) {
                System.err.println("   ⚠️  SKIP : dossier introuvable → "
                        + inputDir.getAbsolutePath());
                continue;
            }

            // Compte les paires originales
            File pairsDir = new File(inputPath + "/pairs");
            int origCount = 0;
            if (pairsDir.exists()) {
                File[] dirs = pairsDir.listFiles(File::isDirectory);
                origCount = dirs != null ? dirs.length : 0;
            }
            totalOriginal += origCount;
            System.out.println("   Paires originales : " + origCount);

            try {
                File outDir = new File(outputDir);
                PairGenerator generator = new PairGenerator(
                        pairsDir.exists() ? pairsDir : inputDir,
                        outDir,
                        maxMut);
                generator.generate();

                int generated = generator.getTotalGenerated();
                totalGenerated += generated;

                System.out.println("   ✅ Paires générées : " + generated);
                System.out.println("   Dataset            : "
                        + outDir.getAbsolutePath());

            } catch (Exception e) {
                System.err.println("   ❌ ERREUR : " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=================================================");
        System.out.println("  RÉSULTAT FINAL TOUTES SOURCES");
        System.out.println("=================================================");
        System.out.println("  Paires originales  : " + totalOriginal);
        System.out.println("  Paires augmentées  : " + totalGenerated);
        System.out.println("  TOTAL DATASET      : " + (totalOriginal + totalGenerated));
        System.out.println("=================================================");
    }
}
