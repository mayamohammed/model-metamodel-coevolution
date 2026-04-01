package com.coevolution.collector.model;

import java.io.File;
import java.time.Instant;

public class PairMetadata {
    public final File v1;
    public final File v2;
    public final String repo;
    public final String path;
    public final String commitBefore;
    public final String commitAfter;
    public final Instant timestamp;

    public PairMetadata(File v1, File v2, String repo, String path,
                        String commitBefore, String commitAfter, Instant timestamp) {
        this.v1 = v1;
        this.v2 = v2;
        this.repo = repo;
        this.path = path;
        this.commitBefore = commitBefore;
        this.commitAfter = commitAfter;
        this.timestamp = timestamp;
    }
}
