package com.coevolution.augmentation.pipeline;

import com.coevolution.augmentation.generator.PairGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class AugmentationMain {

    public static void main(String[] args) throws Exception {
        String inputPath  = args.length > 0 ? args[0] : "../collector/dataset/pairs";
        String outputPath = args.length > 1 ? args[1] : "augmented";
        int maxMutations  = args.length > 2 ? Integer.parseInt(args[2]) : 100;

        System.out.println("=================================================");
        System.out.println("  DATA AUGMENTATION — MAX PAIRES");
        System.out.println("=================================================");
        System.out.println("  Source    : " + inputPath);
        System.out.println("  Output    : " + outputPath);
        System.out.println("  Mutations : " + maxMutations + "/fichier");
        System.out.println("=================================================");

        File inputDir  = new File(inputPath);
        File outputDir = new File(outputPath);
        outputDir.mkdirs();

        if (!inputDir.exists()) {
            System.err.println("❌ Source manquant : " + inputPath);
            System.exit(1);
        }

        // 👇 FIX : trouve TOUS v1.ecore
        int v1Count = countV1Files(inputDir.toPath());
        System.out.println("📊 v1.ecore trouvés : " + v1Count);

        PairGenerator generator = new PairGenerator(inputDir, outputDir, maxMutations);
        generator.generate();

        int original  = (int) Files.walk(inputDir.toPath())
            .filter(Files::isDirectory)
            .count() - 1;  // - pairs/
        int augmented = generator.getTotalGenerated();

        System.out.println("\n=================================================");
        System.out.println("  🎯 RÉSULTAT FINAL");
        System.out.println("=================================================");
        System.out.println("  Paires originales  : " + original);
        System.out.println("  Paires augmentées  : " + augmented);
        System.out.println("  TOTAL DATASET      : " + (original + augmented));
        System.out.println("=================================================");
    }

    /** Compte TOUS v1.ecore récursif */
    private static int countV1Files(Path root) throws IOException {
        return (int) Files.walk(root)
            .filter(p -> {
                String name = p.getFileName().toString();
                return name.equals("v1.ecore") || name.equals("v1_raw.ecore");
            })
            .count();
    }
}
