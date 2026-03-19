package com.coevolution.collector.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * VersionExtractor - utilitaires pour :
 *  - lister les commits qui touchent des fichiers .ecore
 *  - lister l historique d un fichier specifique
 *  - extraire le contenu d un fichier a un commit donne
 */
public class VersionExtractor {

    private final Repository repository;
    private final Git git;

    public VersionExtractor(Repository repository) {
        this.repository = repository;
        this.git = new Git(repository);
    }

    /**
     * Liste tous les commits qui modifient au moins un fichier .ecore.
     * Les commits sont retournes du plus recent au plus ancien.
     */
    public List<RevCommit> listEcoreCommits() throws IOException {
        List<RevCommit> result = new ArrayList<>();

        ObjectId headId = repository.resolve("HEAD");
        if (headId == null) {
            System.out.println("[VersionExtractor] Aucun commit HEAD trouve.");
            return result;
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            revWalk.markStart(revWalk.parseCommit(headId));

            for (RevCommit commit : revWalk) {
                if (commitTouchesEcore(commit)) {
                    result.add(commit);
                }
            }
        }

        System.out.println("[VersionExtractor] Commits touchant .ecore : " + result.size());
        return result;
    }

    /**
     * Verifie si un commit modifie au moins un fichier .ecore
     * en comparant avec son/ses parent(s).
     */
    private boolean commitTouchesEcore(RevCommit commit) throws IOException {
        RevCommit[] parents = commit.getParents();

        // Commit initial : verifier si l arbre contient un .ecore
        if (parents == null || parents.length == 0) {
            return treeContainsEcore(commit.getTree());
        }

        // Comparer avec chaque parent
        try (RevWalk revWalk = new RevWalk(repository)) {
            for (RevCommit parent : parents) {
                revWalk.parseHeaders(parent);
                try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    df.setRepository(repository);
                    List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                    for (DiffEntry diff : diffs) {
                        String newPath = diff.getNewPath();
                        String oldPath = diff.getOldPath();
                        if ((newPath != null && newPath.endsWith(".ecore"))
                                || (oldPath != null && oldPath.endsWith(".ecore"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Verifie si un arbre Git contient au moins un fichier .ecore.
     */
    private boolean treeContainsEcore(RevTree tree) throws IOException {
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                if (treeWalk.getPathString().endsWith(".ecore")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Liste l historique complet d un fichier specifique (git log -- path).
     * Les commits sont retournes du plus recent au plus ancien.
     *
     * @param filePath chemin relatif du fichier dans le depot (ex: model/Company.ecore)
     */
    public List<RevCommit> listFileHistory(String filePath) throws Exception {
        List<RevCommit> result = new ArrayList<>();
        Iterable<RevCommit> logs = git.log()
                .addPath(filePath)
                .call();
        for (RevCommit commit : logs) {
            result.add(commit);
        }
        System.out.println("[VersionExtractor] Historique de " + filePath + " : " + result.size() + " commits");
        return result;
    }

    /**
     * Extrait le contenu (String UTF-8) d un fichier a un commit donne.
     * Retourne null si le fichier n existe pas dans ce commit.
     *
     * @param commit   le commit cible
     * @param filePath chemin relatif du fichier dans le depot
     */
    public String extractFileContent(RevCommit commit, String filePath) throws IOException {
        RevTree tree = commit.getTree();

        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, tree)) {
            if (treeWalk == null) {
                System.out.println("[VersionExtractor] Fichier introuvable dans commit "
                        + commit.getName().substring(0, 7) + " : " + filePath);
                return null;
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(blobId);
            byte[] bytes = loader.getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Retourne les paires consecutives (commitN+1, commitN) pour un fichier donne.
     * Chaque paire = {avant, apres} = {history[i+1], history[i]}
     * La liste est ordonnee de la paire la plus recente a la plus ancienne.
     *
     * @param filePath chemin relatif du fichier .ecore dans le depot
     * @return liste de tableaux [commitAvant, commitApres]
     */
    public List<RevCommit[]> listConsecutivePairs(String filePath) throws Exception {
        List<RevCommit> history = listFileHistory(filePath);
        List<RevCommit[]> pairs = new ArrayList<>();

        // history[0] = le plus recent, history[n-1] = le plus ancien
        // paire : avant = history[i+1], apres = history[i]
        for (int i = 0; i < history.size() - 1; i++) {
            RevCommit after  = history.get(i);
            RevCommit before = history.get(i + 1);
            pairs.add(new RevCommit[]{before, after});
        }

        System.out.println("[VersionExtractor] Paires consecutives pour "
                + filePath + " : " + pairs.size());
        return pairs;
    }

    /**
     * Liste tous les chemins de fichiers .ecore presents dans HEAD.
     */
    public List<String> listEcoreFilesInHead() throws IOException {
        List<String> result = new ArrayList<>();

        ObjectId headId = repository.resolve("HEAD");
        if (headId == null) return result;

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit headCommit = revWalk.parseCommit(headId);
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(headCommit.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String path = treeWalk.getPathString();
                    if (path.endsWith(".ecore")) {
                        result.add(path);
                    }
                }
            }
        }

        System.out.println("[VersionExtractor] Fichiers .ecore dans HEAD : " + result.size());
        return result;
    }
}