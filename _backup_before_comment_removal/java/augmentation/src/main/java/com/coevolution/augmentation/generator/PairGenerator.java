package com.coevolution.augmentation.generator;

import com.coevolution.augmentation.mutation.MutationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PairGenerator {

    private final EcoreMutationEngine engine;
    private final File inputDir;
    private final File outputDir;
    private final int  maxMutations;
    private final ObjectMapper mapper;
    private int totalGenerated = 0;
    private int totalFailed    = 0;

    public PairGenerator(File inputDir, File outputDir, int maxMutations) {
        this.engine       = new EcoreMutationEngine();
        this.inputDir     = inputDir;
        this.outputDir    = outputDir;
        this.maxMutations = maxMutations;
        this.mapper       = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        
        Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .put("ecore", new XMIResourceFactoryImpl());
        EPackage.Registry.INSTANCE
                .put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
    }

    public void generate() throws Exception {
        outputDir.mkdirs();
        List<File> v1Files = collectV1Files(inputDir);
        System.out.println("[PairGenerator] " + v1Files.size()
                + " fichiers v1.ecore trouves");
        System.out.println("[PairGenerator] Max mutations/fichier : "
                + maxMutations);

        for (File v1 : v1Files) {
            try {
                generatePairsForFile(v1);
            } catch (Exception e) {
                System.err.println("[ERREUR] " + v1.getName()
                        + " : " + e.getMessage());
                totalFailed++;
            }
        }
        printSummary(v1Files.size());
    }

    private void generatePairsForFile(File v1File) throws Exception {
        
        List<EcoreMutationEngine.MutationCandidate> candidates =
                engine.generateMutations(v1File);

        int count = 0;
        for (EcoreMutationEngine.MutationCandidate c : candidates) {
            if (count >= maxMutations) break;

            String pairId = String.format("augmented_%04d_%s_from_%s",
                    totalGenerated,
                    c.result.getChangeType().toLowerCase(),
                    v1File.getParentFile().getName());

            File pairDir = new File(outputDir, pairId);
            pairDir.mkdirs();

            
            engine.saveEcore(c.originalPackage, new File(pairDir, "v1.ecore"));
            engine.saveEcore(c.mutatedPackage,  new File(pairDir, "v2.ecore"));

            saveManifest(pairDir, pairId, v1File, c.result);
            totalGenerated++;
            count++;
        }
    }

    private void saveManifest(File dir, String pairId,
                               File v1File, MutationResult r) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("pair_id",     pairId);
        root.put("source",      "augmented");
        root.put("v1",          "v1.ecore");
        root.put("v2",          "v2.ecore");
        root.put("origin_file", v1File.getAbsolutePath());
        root.put("timestamp",   LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        ObjectNode gt = mapper.createObjectNode();
        gt.put("change_type", r.getChangeType());
        gt.put("element",     r.getElement()     != null ? r.getElement()     : "");
        gt.put("context",     r.getContext()     != null ? r.getContext()     : "");
        gt.put("old_value",   r.getOldValue()    != null ? r.getOldValue()    : "");
        gt.put("new_value",   r.getNewValue()    != null ? r.getNewValue()    : "");
        gt.put("description", r.getDescription() != null ? r.getDescription() : "");
        root.set("ground_truth", gt);
        mapper.writeValue(new File(dir, "manifest.json"), root);
    }

    private List<File> collectV1Files(File dir) {
        List<File> res = new ArrayList<>();
        if (!dir.exists()) return res;
        File[] dirs = dir.listFiles();
        if (dirs == null) return res;
        for (File d : dirs) {
            if (!d.isDirectory()) continue;
            File v1 = new File(d, "v1.ecore");
            if (v1.exists()) res.add(v1);
        }
        return res;
    }

    private void printSummary(int nb) {
        System.out.println("\n=================================================");
        System.out.println("  Fichiers traites  : " + nb);
        System.out.println("  Paires generees   : " + totalGenerated);
        System.out.println("  Echecs            : " + totalFailed);
        System.out.println("=================================================");
    }

    public int getTotalGenerated() { return totalGenerated; }
}

