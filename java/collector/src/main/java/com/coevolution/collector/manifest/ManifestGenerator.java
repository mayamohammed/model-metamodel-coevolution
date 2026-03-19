package com.coevolution.collector.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ManifestGenerator {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final ObjectMapper mapper;

    public ManifestGenerator() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    
    public void generate(String repoName, String filePath,
                         String commitBefore, String commitAfter,
                         File v1, File v2, File outputDir) throws Exception {

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        
        String shaV1 = sha256(v1);
        String shaV2 = sha256(v2);

        
        boolean identical = shaV1.equals(shaV2);

        
        String pairId = buildPairId(repoName, filePath, commitBefore, commitAfter);

        
        ObjectNode node = mapper.createObjectNode();
        node.put("pair_id",       pairId);
        node.put("repo",          repoName);
        node.put("file_path",     filePath);
        node.put("commit_before", commitBefore);
        node.put("commit_after",  commitAfter);
        node.put("sha_v1",        shaV1);
        node.put("sha_v2",        shaV2);
        node.put("size_v1",       v1.length());
        node.put("size_v2",       v2.length());
        node.put("timestamp",     ISO_FMT.format(Instant.now()));
        node.put("identical",     identical);

        
        File manifestFile = new File(outputDir, "manifest.json");
        mapper.writeValue(manifestFile, node);

        System.out.println("[ManifestGenerator] Manifest ecrit : " + manifestFile.getAbsolutePath()
                + (identical ? " [IDENTIQUE - a filtrer]" : ""));
    }

    
    private String buildPairId(String repoName, String filePath,
                                String commitBefore, String commitAfter) {
        String safeRepo = repoName.replaceAll("[/\\\\]", "_");
        String safeFile = filePath.replaceAll("[/\\\\]", "_")
                                  .replace(".ecore", "");
        String before7  = commitBefore.length() >= 7 ? commitBefore.substring(0, 7) : commitBefore;
        String after7   = commitAfter.length()  >= 7 ? commitAfter.substring(0, 7)  : commitAfter;
        return safeRepo + "__" + safeFile + "__" + before7 + "__" + after7;
    }

    
    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(file.toPath());
        byte[] hash  = digest.digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
