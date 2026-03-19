package com.coevolution.collector.git;

public class TestMain {
    public static void main(String[] args) throws Exception {
        GitRepositoryManager mgr = new GitRepositoryManager();
        mgr.scanAllRepos("../../data");
        mgr.close();
    }
}

