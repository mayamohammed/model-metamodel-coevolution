package com.coevolution.collector.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class GitRepositoryManager {

    private List<Repository> repositories = new ArrayList<>();
    private List<Git>        gits         = new ArrayList<>();
    private int totalEcoreFiles = 0;
    private File reposRoot = null; // FIX : pour getReposRoot()

    public GitRepositoryManager() {}

    public void scanAllRepos(String dataRootPath) throws IOException {
        reposRoot = new File(dataRootPath); // FIX : sauvegarde root
        Path dataPath = Paths.get(dataRootPath);

        Files.walk(dataPath)
            .filter(p -> {
                String fileName = p.getFileName().toString();
                if (fileName.endsWith(".ecore")) {
                    totalEcoreFiles++;
                    return false;
                }
                return Files.isDirectory(p) && fileName.equals(".git");
            })
            .forEach(this::openRepo);

        System.out.println("\n🎯 RÉSULTAT FINAL:");
        System.out.println("   Ecore trouvés : " + totalEcoreFiles);
        System.out.println("   Repos Git     : " + repositories.size());
        System.out.println("====================");
    }

    private void openRepo(Path gitDirPath) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
                    .setMustExist(true)
                    .setGitDir(gitDirPath.toFile());
            Repository repo = builder.build();
            Git git = new Git(repo);
            repositories.add(repo);
            gits.add(git);
            System.out.println("✅ " + repo.getDirectory().getParentFile().getName());
        } catch (IOException e) {
            System.err.println("❌ Skip " + gitDirPath.getParent() + " → " + e.getMessage());
        }
    }

    public void openLocal(String repoPath) throws IOException {
        scanAllRepos(repoPath);
    }

    /**
     * FIX : retourne 1er repo ou NULL (pas crash)
     */
    public Repository getRepository() {
        if (repositories.isEmpty()) {
            System.out.println("[GitRepositoryManager] Mode disque pur - pas de Git");
            return null; // FIX : null au lieu de crash
        }
        return repositories.get(0);
    }

    /**
     * FIX : retourne root pour Mode C
     */
    public File getReposRoot() {
        return reposRoot;
    }

    public List<Repository> getAllRepositories() {
        return repositories;
    }

    public Git getGit() {
        if (gits.isEmpty()) return null; // FIX : null au lieu de crash
        return gits.get(0);
    }

    public int getTotalEcoreFiles() {
        return totalEcoreFiles;
    }

    public void close() { closeAll(); }

    public void closeAll() {
        gits.forEach(Git::close);
        repositories.forEach(Repository::close);
        System.out.println("🔒 Fermé " + repositories.size() + " repos");
        repositories.clear();
        gits.clear();
    }
}
