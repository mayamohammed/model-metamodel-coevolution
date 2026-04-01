package com.coevolution.collector.tests;

import com.coevolution.collector.git.GitRepositoryManager;
import java.io.IOException;

public class TestMainSimple {
    public static void main(String[] args) throws IOException {
        GitRepositoryManager mgr = new GitRepositoryManager();
        mgr.scanAllRepos("../../data");
        mgr.close();
        System.out.println("âœ… TEST TERMINÃ‰ ! Total Ecore: " + mgr.getTotalEcoreFiles());
    }
}

