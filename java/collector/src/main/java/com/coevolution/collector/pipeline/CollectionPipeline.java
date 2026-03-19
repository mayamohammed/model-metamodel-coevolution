package com.coevolution.collector.pipeline;

import com.coevolution.collector.git.GitRepositoryManager;
import com.coevolution.collector.git.VersionExtractor;
import com.coevolution.collector.manifest.ManifestGenerator;
import com.coevolution.collector.normalizer.EMFNormalizer;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class CollectionPipeline {

    private final GitRepositoryManager gitManager;
    private final File outputDir;
    private int maxPairs = 100;

    private final EMFNormalizer normalizer;
    private final ManifestGenerator manifestGenerator;

    private int pairsCollected = 0;
    private int pairsSkipped   = 0;
    private int pairsIdentical = 0;
    private int pairsInvalid   = 0;

    public CollectionPipeline(GitRepositoryManager gitManager, File outputDir) {
        this.gitManager        = gitManager;
        this.outputDir         = outputDir;
        this.normalizer        = new EMFNormalizer();
        this.manifestGenerator = new ManifestGenerator();
    }

    public void setMaxPairs(int maxPairs) { this.maxPairs = maxPairs; }

    public void run() throws Exception {
        System.out.println("=================================================");
        System.out.println("[CollectionPipeline] Demarrage du pipeline");
        System.out.println("[CollectionPipeline] Output   : " + outputDir.getAbsolutePath());
        System.out.println("[CollectionPipeline] MaxPairs : " + maxPairs);
        System.out.println("=================================================");

        File pairsDir = new File(outputDir, "pairs");
        if (!pairsDir.exists()) pairsDir.mkdirs();

        Repository repo = null;
        try { repo = gitManager.getRepository(); } catch (Exception e) {  }

        if (repo != null) {
            VersionExtractor extractor = new VersionExtractor(repo);
            String repoName = repo.getDirectory().getParentFile().getName();

            List<String> ecoreFiles = extractor.listEcoreFilesInHead();
            System.out.println("[CollectionPipeline] Fichiers .ecore trouves : " + ecoreFiles.size());

            int gitPairs = countGitHistoryPairs(extractor, ecoreFiles);
            System.out.println("[CollectionPipeline] Mode A (Git history) : " + gitPairs + " paires potentielles");

            if (gitPairs > 0) {
                runModeA(extractor, ecoreFiles, repoName, pairsDir);
            }

            if (pairsCollected < maxPairs) {
                System.out.println("\n[CollectionPipeline] Mode B (noms _vN) : detection des paires...");
                runModeB(ecoreFiles, repoName, pairsDir);
            }

        } else {
            
            System.out.println("[CollectionPipeline] Mode DISQUE PUR (pas de Git)");
            File diskRoot = gitManager.getReposRoot();
            if (diskRoot != null && diskRoot.exists()) {
                runModeDisk(diskRoot, pairsDir);
            } else {
                System.out.println("[Pipeline-D] ERREUR : rootDir null !");
            }
        }

        if (pairsCollected < maxPairs) {
            File reposRoot = gitManager.getReposRoot();
            if (reposRoot != null && reposRoot.exists()) {
                System.out.println("\n[CollectionPipeline] Mode C (repos externes sur disque)...");
                runModeC(reposRoot, pairsDir);
            }
        }

        printStats();
    }

    
    
    

    private void runModeDisk(File rootDir, File pairsDir) {
        System.out.println("[Pipeline-D] Scan disque : " + rootDir.getAbsolutePath());
        List<File> ecoreFiles = findEcoreFiles(rootDir);
        System.out.println("[Pipeline-D] Ecore trouves : " + ecoreFiles.size());

        Map<String, List<File>> groups = groupFilesByPrefix(ecoreFiles);
        System.out.println("[Pipeline-D] Groupes _vN : " + groups.size());

        for (Map.Entry<String, List<File>> entry : groups.entrySet()) {
            if (pairsCollected >= maxPairs) break;
            List<File> versions = entry.getValue();
            Collections.sort(versions, Comparator.comparing(File::getName));
            for (int i = 0; i < versions.size() - 1; i++) {
                if (pairsCollected >= maxPairs) break;
                processPairFromDisk(rootDir.getName(),
                        versions.get(i), versions.get(i + 1), pairsDir);
            }
        }
    }

    
    
    

    private int countGitHistoryPairs(VersionExtractor extractor,
                                      List<String> ecoreFiles) throws Exception {
        int total = 0;
        for (String f : ecoreFiles) {
            List<RevCommit> history = extractor.listFileHistory(f);
            if (history.size() >= 2) total += history.size() - 1;
        }
        return total;
    }

    private void runModeA(VersionExtractor extractor, List<String> ecoreFiles,
                           String repoName, File pairsDir) throws Exception {
        for (String ecoreFile : ecoreFiles) {
            if (pairsCollected >= maxPairs) break;
            List<RevCommit[]> pairs = extractor.listConsecutivePairs(ecoreFile);
            for (RevCommit[] pair : pairs) {
                if (pairsCollected >= maxPairs) break;
                processPairGit(extractor, repoName, ecoreFile, pair[0], pair[1], pairsDir);
            }
        }
    }

    private void processPairGit(VersionExtractor extractor,
                                  String repoName, String ecoreFile,
                                  RevCommit commitBefore, RevCommit commitAfter,
                                  File pairsDir) {
        String before7   = commitBefore.getName().substring(0, 7);
        String after7    = commitAfter.getName().substring(0, 7);
        String pairLabel = ecoreFile.replace("/", "_").replace(".ecore", "")
                + "__" + before7 + "__" + after7;
        try {
            String cv1 = extractor.extractFileContent(commitBefore, ecoreFile);
            String cv2 = extractor.extractFileContent(commitAfter,  ecoreFile);
            if (cv1 == null || cv2 == null) { pairsSkipped++;   return; }
            if (cv1.equals(cv2))            { pairsIdentical++; return; }

            File pairDir = new File(pairsDir, pairLabel);
            pairDir.mkdirs();
            File rawV1 = new File(pairDir, "v1_raw.ecore");
            File rawV2 = new File(pairDir, "v2_raw.ecore");
            Files.write(rawV1.toPath(), cv1.getBytes(StandardCharsets.UTF_8));
            Files.write(rawV2.toPath(), cv2.getBytes(StandardCharsets.UTF_8));

            File normV1 = new File(pairDir, "v1.ecore");
            File normV2 = new File(pairDir, "v2.ecore");
            normalizer.normalize(rawV1, normV1);
            normalizer.normalize(rawV2, normV2);

            manifestGenerator.generate(repoName, ecoreFile,
                    commitBefore.getName(), commitAfter.getName(),
                    normV1, normV2, pairDir);

            pairsCollected++;
            System.out.println("[Pipeline-A] Paire OK (" + pairsCollected + ") : " + pairLabel);

        } catch (Exception e) {
            System.out.println("[Pipeline-A] ERREUR : " + pairLabel + " -> " + e.getMessage());
            pairsInvalid++;
        }
    }

    
    
    

    private void runModeB(List<String> ecoreFiles, String repoName,
                           File pairsDir) throws Exception {
        Map<String, List<String>> groups = groupByPrefix(ecoreFiles);
        System.out.println("[Pipeline-B] Groupes detectes : " + groups.size());
        Repository repo = gitManager.getRepository();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (pairsCollected >= maxPairs) break;
            List<String> versions = entry.getValue();
            Collections.sort(versions);
            for (int i = 0; i < versions.size() - 1; i++) {
                if (pairsCollected >= maxPairs) break;
                processPairByName(repo, repoName,
                        versions.get(i), versions.get(i + 1), pairsDir);
            }
        }
    }

    private Map<String, List<String>> groupByPrefix(List<String> ecoreFiles) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String path : ecoreFiles) {
            String name   = path.substring(path.lastIndexOf("/") + 1);
            String dir    = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
            String prefix = name.replaceAll("_v\\d+\\.ecore$", "");
            if (prefix.equals(name.replace(".ecore", ""))) continue;
            String groupKey = dir + "/" + prefix;
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(path);
        }
        return groups;
    }

    private void processPairByName(Repository repo, String repoName,
                                    String pathV1, String pathV2, File pairsDir) {
        String label = pathV1.replace("/", "_").replace(".ecore", "")
                + "__to__" + pathV2.replace("/", "_").replace(".ecore", "");
        try {
            String cv1 = readFileFromHead(repo, pathV1);
            String cv2 = readFileFromHead(repo, pathV2);
            if (cv1 == null || cv2 == null) { pairsSkipped++;   return; }
            if (cv1.equals(cv2))            { pairsIdentical++; return; }

            File pairDir = new File(pairsDir, label);
            pairDir.mkdirs();
            Files.write(new File(pairDir, "v1_raw.ecore").toPath(), cv1.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(pairDir, "v2_raw.ecore").toPath(), cv2.getBytes(StandardCharsets.UTF_8));

            File normV1 = new File(pairDir, "v1.ecore");
            File normV2 = new File(pairDir, "v2.ecore");
            normalizer.normalize(new File(pairDir, "v1_raw.ecore"), normV1);
            normalizer.normalize(new File(pairDir, "v2_raw.ecore"), normV2);

            manifestGenerator.generate(repoName, pathV1 + " -> " + pathV2,
                    "HEAD:" + pathV1, "HEAD:" + pathV2, normV1, normV2, pairDir);

            pairsCollected++;
            System.out.println("[Pipeline-B] Paire OK (" + pairsCollected + ") : " + label);

        } catch (Exception e) {
            System.out.println("[Pipeline-B] ERREUR : " + label + " -> " + e.getMessage());
            pairsInvalid++;
        }
    }

    private String readFileFromHead(Repository repo, String filePath) throws Exception {
        ObjectId headId = repo.resolve("HEAD");
        if (headId == null) return null;
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit head = revWalk.parseCommit(headId);
            try (TreeWalk treeWalk = TreeWalk.forPath(repo, filePath, head.getTree())) {
                if (treeWalk == null) return null;
                byte[] bytes = repo.open(treeWalk.getObjectId(0)).getBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    
    
    

    private void runModeC(File reposRoot, File pairsDir) {
        File[] repos = reposRoot.listFiles(File::isDirectory);
        if (repos == null) return;

        for (File repoDir : repos) {
            if (pairsCollected >= maxPairs) break;
            System.out.println("\n[Pipeline-C] Scan repo : " + repoDir.getName());

            List<File> ecoreFiles = findEcoreFiles(repoDir);
            System.out.println("[Pipeline-C] Fichiers .ecore trouves : " + ecoreFiles.size());
            if (ecoreFiles.isEmpty()) continue;

            Map<String, List<File>> groups = groupFilesByPrefix(ecoreFiles);
            System.out.println("[Pipeline-C] Groupes _vN detectes : " + groups.size());

            for (Map.Entry<String, List<File>> entry : groups.entrySet()) {
                if (pairsCollected >= maxPairs) break;
                List<File> versions = entry.getValue();
                Collections.sort(versions, Comparator.comparing(File::getName));
                for (int i = 0; i < versions.size() - 1; i++) {
                    if (pairsCollected >= maxPairs) break;
                    processPairFromDisk(repoDir.getName(),
                            versions.get(i), versions.get(i + 1), pairsDir);
                }
            }

            if (pairsCollected < maxPairs) {
                runModeAOnExternalRepo(repoDir, pairsDir);
            }
        }
    }

    private List<File> findEcoreFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().equals(".git")) {
                result.addAll(findEcoreFiles(f));
            } else if (f.isFile() && f.getName().endsWith(".ecore")) {
                result.add(f);
            }
        }
        return result;
    }

    private Map<String, List<File>> groupFilesByPrefix(List<File> files) {
        Map<String, List<File>> groups = new HashMap<>();
        for (File f : files) {
            String name   = f.getName();
            String prefix = name.replaceAll("_v\\d+\\.ecore$", "");
            if (prefix.equals(name.replace(".ecore", ""))) continue;
            String key = f.getParent() + "/" + prefix;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(f);
        }
        return groups;
    }

    private void processPairFromDisk(String repoName, File fileV1,
                                      File fileV2, File pairsDir) {
        String label = repoName + "__"
                + fileV1.getName().replace(".ecore", "")
                + "__to__"
                + fileV2.getName().replace(".ecore", "");
        try {
            String cv1 = new String(Files.readAllBytes(fileV1.toPath()), StandardCharsets.UTF_8);
            String cv2 = new String(Files.readAllBytes(fileV2.toPath()), StandardCharsets.UTF_8);
            if (cv1.equals(cv2)) { pairsIdentical++; return; }

            File pairDir = new File(pairsDir, label);
            pairDir.mkdirs();
            File rawV1 = new File(pairDir, "v1_raw.ecore");
            File rawV2 = new File(pairDir, "v2_raw.ecore");
            Files.write(rawV1.toPath(), cv1.getBytes(StandardCharsets.UTF_8));
            Files.write(rawV2.toPath(), cv2.getBytes(StandardCharsets.UTF_8));

            File normV1 = new File(pairDir, "v1.ecore");
            File normV2 = new File(pairDir, "v2.ecore");
            try {
                normalizer.normalize(rawV1, normV1);
                normalizer.normalize(rawV2, normV2);
            } catch (Exception e) { pairsInvalid++; return; }

            if (!normalizer.isValidEcore(normV1) || !normalizer.isValidEcore(normV2)) {
                pairsInvalid++; return;
            }

            manifestGenerator.generate(repoName,
                    fileV1.getName() + " -> " + fileV2.getName(),
                    "disk:" + fileV1.getAbsolutePath(),
                    "disk:" + fileV2.getAbsolutePath(),
                    normV1, normV2, pairDir);

            pairsCollected++;
            System.out.println("[Pipeline-C/D] Paire OK (" + pairsCollected + ") : " + label);

        } catch (Exception e) {
            System.out.println("[Pipeline-C/D] ERREUR : " + label + " -> " + e.getMessage());
            pairsInvalid++;
        }
    }

    private void runModeAOnExternalRepo(File repoDir, File pairsDir) {
        GitRepositoryManager extGit = new GitRepositoryManager();
        try {
            extGit.openLocal(repoDir.getAbsolutePath());
            VersionExtractor extExtractor = new VersionExtractor(extGit.getRepository());
            String repoName = repoDir.getName();

            List<String> ecoreFiles = extExtractor.listEcoreFilesInHead();
            System.out.println("[Pipeline-C/A] " + repoName
                    + " : " + ecoreFiles.size() + " .ecore dans HEAD");

            for (String ecoreFile : ecoreFiles) {
                if (pairsCollected >= maxPairs) break;
                try {
                    List<RevCommit[]> pairs = extExtractor.listConsecutivePairs(ecoreFile);
                    for (RevCommit[] pair : pairs) {
                        if (pairsCollected >= maxPairs) break;
                        processPairGit(extExtractor, repoName, ecoreFile,
                                pair[0], pair[1], pairsDir);
                    }
                } catch (Exception e) {  }
            }
        } catch (Exception e) {
            System.out.println("[Pipeline-C/A] Impossible d ouvrir : "
                    + repoDir.getName() + " -> " + e.getMessage());
        } finally {
            extGit.close();
        }
    }

    
    
    

    public int getPairsCollected()  { return pairsCollected;  }
    public int getPairsSkipped()    { return pairsSkipped;    }
    public int getPairsIdentical()  { return pairsIdentical;  }
    public int getPairsInvalid()    { return pairsInvalid;    }

    private void printStats() {
        System.out.println("\n=================================================");
        System.out.println("[CollectionPipeline] Pipeline termine !");
        System.out.println("  Paires collectees  : " + pairsCollected);
        System.out.println("  Paires identiques  : " + pairsIdentical);
        System.out.println("  Paires invalides   : " + pairsInvalid);
        System.out.println("  Paires skippees    : " + pairsSkipped);
        System.out.println("=================================================");
    }
}

