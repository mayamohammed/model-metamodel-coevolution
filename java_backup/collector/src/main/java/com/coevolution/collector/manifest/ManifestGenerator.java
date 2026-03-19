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

/**
 * ManifestGenerator - genere un fichier manifest.json
 * pour chaque paire (v1, v2) de fichiers .ecore collectes.
 *
 * Champs du manifest :
 *  - pair_id       : identifiant unique de la paire
 *  - repo          : nom du depot
 *  - file_path     : chemin relatif du fichier .ecore dans le depot
 *  - commit_before : SHA du commit avant (v1)
 *  - commit_after  : SHA du commit apres (v2)
 *  - sha_v1        : SHA-256 du contenu de v1
 *  - sha_v2        : SHA-256 du contenu de v2
 *  - size_v1       : taille en octets de v1
 *  - size_v2       : taille en octets de v2
 *  - timestamp     : date/heure de generation du manifest
 *  - identical     : true si v1 et v2 sont identiques (a filtrer)
 */
public class ManifestGenerator {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final ObjectMapper mapper;

    public ManifestGenerator() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Genere un manifest.json dans outputDir pour la paire (v1, v2).
     *
     * @param repoName     identifiant du depot (ex: "eclipse/emf")
     * @param filePath     chemin relatif du .ecore dans le depot (ex: "model/Company.ecore")
     * @param commitBefore SHA complet du commit avant
     * @param commitAfter  SHA complet du commit apres
     * @param v1           fichier .ecore version avant
     * @param v2           fichier .ecore version apres
     * @param outputDir    dossier de sortie ou ecrire manifest.json
     * @throws Exception   en cas d erreur I/O ou JSON
     */
    public void generate(String repoName, String filePath,
                         String commitBefore, String commitAfter,
                         File v1, File v2, File outputDir) throws Exception {

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Calculer les SHA-256 des fichiers
        String shaV1 = sha256(v1);
        String shaV2 = sha256(v2);

        // Verifier si les deux fichiers sont identiques
        boolean identical = shaV1.equals(shaV2);

        // Construire l identifiant unique de la paire
        String pairId = buildPairId(repoName, filePath, commitBefore, commitAfter);

        // Construire le JSON
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

        // Ecrire manifest.json dans outputDir
        File manifestFile = new File(outputDir, "manifest.json");
        mapper.writeValue(manifestFile, node);

        System.out.println("[ManifestGenerator] Manifest ecrit : " + manifestFile.getAbsolutePath()
                + (identical ? " [IDENTIQUE - a filtrer]" : ""));
    }

    /**
     * Construit un identifiant unique pour la paire.
     * Format : repoName__filePath__before7__after7
     * (les slashes sont remplacés par des underscores)
     */
    private String buildPairId(String repoName, String filePath,
                                String commitBefore, String commitAfter) {
        String safeRepo = repoName.replaceAll("[/\\\\]", "_");
        String safeFile = filePath.replaceAll("[/\\\\]", "_")
                                  .replace(".ecore", "");
        String before7  = commitBefore.length() >= 7 ? commitBefore.substring(0, 7) : commitBefore;
        String after7   = commitAfter.length()  >= 7 ? commitAfter.substring(0, 7)  : commitAfter;
        return safeRepo + "__" + safeFile + "__" + before7 + "__" + after7;
    }

    /**
     * Calcule le SHA-256 d un fichier et retourne sa representation hexadecimale.
     *
     * @param file fichier a hasher
     * @return SHA-256 en hexadecimal (64 caracteres)
     */
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