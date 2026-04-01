package com.coevolution.migrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransformationGenerator {

    private final String transformationsDir;

    private static final Map<String, String> LABEL_TO_ATL = new LinkedHashMap<>();

    static {
        LABEL_TO_ATL.put("ECLASS_ADDED",                    "add_class_migration.atl");
        LABEL_TO_ATL.put("ECLASS_REMOVED",                  "remove_class_migration.atl");
        LABEL_TO_ATL.put("EATTRIBUTE_ADDED",                "add_attribute_migration.atl");
        LABEL_TO_ATL.put("EATTRIBUTE_REMOVED",              "remove_attribute_migration.atl");
        LABEL_TO_ATL.put("EATTRIBUTE_TYPE_CHANGED",         "change_attribute_type_migration.atl");
        LABEL_TO_ATL.put("EREFERENCE_ADDED",                "add_reference_migration.atl");
        LABEL_TO_ATL.put("EREFERENCE_REMOVED",              "remove_reference_migration.atl");
        LABEL_TO_ATL.put("EREFERENCE_MULTIPLICITY_CHANGED", "change_reference_multiplicity_migration.atl");
        LABEL_TO_ATL.put("EREFERENCE_CONTAINMENT_CHANGED",  "change_reference_containment_migration.atl");
        LABEL_TO_ATL.put("ECLASS_ABSTRACT_CHANGED",         "change_class_abstract_migration.atl");
        LABEL_TO_ATL.put("ECLASS_SUPERTYPE_ADDED",          "add_supertype_migration.atl");
        LABEL_TO_ATL.put("MIXED",                           "mixed_changes_migration.atl");
        LABEL_TO_ATL.put("NO_CHANGE",                       "identity_migration.atl");
    }

    public TransformationGenerator(String transformationsDir) {
        this.transformationsDir = transformationsDir;
        new File(transformationsDir).mkdirs();
    }

    public String selectAtlFile(String predictionLabel) {
        String atlFile = LABEL_TO_ATL.getOrDefault(predictionLabel, "mixed_changes_migration.atl");
        return transformationsDir + File.separator + atlFile;
    }

    public String readAtlContent(String atlPath) throws IOException {
        Path p = Paths.get(atlPath);
        if (!Files.exists(p)) {
            throw new FileNotFoundException("ATL not found: " + atlPath);
        }
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    public MigrationPlan generatePlan(String sourceModelPath,
                                      String targetModelPath,
                                      String predictionLabel,
                                      double confidence,
                                      List<String> deltas) {

        List<String> safeDeltas = (deltas != null) ? deltas : new ArrayList<>();
        DeltaInfo info = parseDeltaInfo(safeDeltas);

        File dir = new File(transformationsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Nom ATL = nom_v1_sans_extension + date
        String v1BaseName = new File(sourceModelPath).getName().replaceAll("_v[0-9]+\\.ecore$", "").replace(".ecore", "");
        String atlDate = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String atlFileName = v1BaseName + "_" + atlDate + ".atl";

        File atlFile = new File(dir, atlFileName);
        String moduleName = atlFileName.replace(".atl", "");

        String atlContent = buildAtl(
                predictionLabel,
                moduleName,
                info,
                sourceModelPath,
                targetModelPath
        );

        System.out.println("=================================");
        System.out.println("[TG] GENERATE PLAN");
        System.out.println("[TG] Dir        = " + dir.getAbsolutePath());
        System.out.println("[TG] ATL file   = " + atlFile.getAbsolutePath());
        System.out.println("[TG] Content    = " + atlContent.length() + " chars");
        System.out.println("=================================");

        try {
            String finalContent = atlContent.endsWith("\n")
                    ? atlContent
                    : atlContent + "\n";
            System.out.println("[DEBUG] Chemin ABSOLU ciblé : " + atlFile.getAbsolutePath());
            Files.write(
                    atlFile.toPath(),
                    finalContent.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            System.out.println("[TG] File written successfully");
            System.out.println("[TG] Exists = " + atlFile.exists());
            System.out.println("[TG] Size   = " + atlFile.length());

        } catch (IOException ex) {
            System.err.println("[TG] ERROR writing ATL file");
            ex.printStackTrace();
        }

        String outputPath = new File(sourceModelPath)
                .getAbsolutePath()
                .replace(".ecore", "_migrated.ecore");

        MigrationPlan plan = new MigrationPlan();
        plan.setSourceModel(sourceModelPath);
        plan.setTargetModel(targetModelPath);
        plan.setOutputModel(outputPath);
        plan.setAtlFile(atlFileName);
        plan.setAtlPath(atlFile.getAbsolutePath());
        plan.setPredictionLabel(predictionLabel);
        plan.setConfidence(confidence);
        plan.setTimestamp(System.currentTimeMillis());

        for (String d : safeDeltas) {
            plan.addAction(d);
        }

        System.out.printf(
                "[TG] Plan created: %s -> %s (ATL=%s)%n",
                new File(sourceModelPath).getName(),
                new File(outputPath).getName(),
                atlFileName
        );

        return plan;
    }

    private String buildAtl(String label,
                            String mod,
                            DeltaInfo info,
                            String srcPath,
                            String tgtPath) {

        StringBuilder sb = new StringBuilder();

        sb.append("-- ============================================================\n");
        sb.append("-- ").append(mod).append(".atl\n");
        sb.append("-- Label  : ").append(label).append("\n");
        sb.append("-- Source : ").append(fname(srcPath)).append("\n");
        sb.append("-- Target : ").append(fname(tgtPath)).append("\n");
        sb.append("-- Deltas : ").append(info.total).append("\n");
        sb.append("--   +classes : ").append(info.addedClasses).append("\n");
        sb.append("--   -classes : ").append(info.removedClasses).append("\n");
        sb.append("--   +attrs   : ").append(info.addedAttrs).append("\n");
        sb.append("--   -attrs   : ").append(info.removedAttrs).append("\n");
        sb.append("--   renames  : ").append(info.renames).append("\n");
        sb.append("--   +refs    : ").append(info.addedRefs).append("\n");
        sb.append("--   -refs    : ").append(info.removedRefs).append("\n");
        sb.append("-- ============================================================\n");
        sb.append("module ").append(mod).append(";\n");
        sb.append("create OUT : Ecore from IN : Ecore;\n\n");

        switch (label) {
            case "ECLASS_ADDED":
                buildAddClass(sb, info);
                break;
            case "ECLASS_REMOVED":
                buildRemoveClass(sb, info);
                break;
            case "EATTRIBUTE_ADDED":
                buildAddAttr(sb, info);
                break;
            case "EATTRIBUTE_REMOVED":
                buildRemoveAttr(sb, info);
                break;
            case "EATTRIBUTE_TYPE_CHANGED":
                buildChangeType(sb, info);
                break;
            case "EREFERENCE_ADDED":
                buildAddRef(sb, info);
                break;
            case "EREFERENCE_REMOVED":
                buildRemoveRef(sb, info);
                break;
            case "EREFERENCE_MULTIPLICITY_CHANGED":
                buildMultiplicity(sb, info);
                break;
            case "EREFERENCE_CONTAINMENT_CHANGED":
                buildContainment(sb, info);
                break;
            case "ECLASS_ABSTRACT_CHANGED":
                buildAbstract(sb, info);
                break;
            case "ECLASS_SUPERTYPE_ADDED":
                buildSupertype(sb, info);
                break;
            case "MIXED":
                buildMixed(sb, info);
                break;
            default:
                buildIdentity(sb);
                break;
        }

        return sb.toString();
    }

    private void buildAddClass(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Classes added: ").append(info.addedClasses).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void buildRemoveClass(StringBuilder sb, DeltaInfo info) {
        sb.append("helper def : classesToRemove : Set(String) = ")
          .append(toSet(info.removedClasses))
          .append(";\n\n");

        copyPackage(sb, true);

        sb.append("rule CopyEClass {\n");
        sb.append("    from s : Ecore!EClass (\n");
        sb.append("        not thisModule.classesToRemove->includes(s.name)\n");
        sb.append("    )\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name                <- s.name,\n");
        sb.append("        abstract            <- s.abstract,\n");
        sb.append("        eSuperTypes         <- s.eSuperTypes\n");
        sb.append("            ->select(st | not thisModule.classesToRemove->includes(st.name)),\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("    )\n}\n\n");

        copyAttr(sb);
        copyRef(sb);
    }

    private void buildAddAttr(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Attributes added: ").append(info.addedAttrs).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void buildRemoveAttr(StringBuilder sb, DeltaInfo info) {
        sb.append("helper def : attrsToRemove : Set(String) = ")
          .append(toSet(info.removedAttrs))
          .append(";\n\n");

        copyPackage(sb, false);

        sb.append("rule CopyEClass {\n");
        sb.append("    from s : Ecore!EClass\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        abstract    <- s.abstract,\n");
        sb.append("        eSuperTypes <- s.eSuperTypes,\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("            ->reject(f | f.oclIsKindOf(Ecore!EAttribute)\n");
        sb.append("                     and thisModule.attrsToRemove->includes(f.name))\n");
        sb.append("    )\n}\n\n");

        copyAttr(sb);
        copyRef(sb);
    }

    private void buildChangeType(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Type changes: ").append(info.typeChanges).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void buildAddRef(StringBuilder sb, DeltaInfo info) {
        sb.append("-- References added: ").append(info.addedRefs).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void buildRemoveRef(StringBuilder sb, DeltaInfo info) {
        sb.append("helper def : refsToRemove : Set(String) = ")
          .append(toSet(info.removedRefs))
          .append(";\n\n");

        copyPackage(sb, false);

        sb.append("rule CopyEClass {\n");
        sb.append("    from s : Ecore!EClass\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        abstract    <- s.abstract,\n");
        sb.append("        eSuperTypes <- s.eSuperTypes,\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("            ->reject(f | f.oclIsKindOf(Ecore!EReference)\n");
        sb.append("                     and thisModule.refsToRemove->includes(f.name))\n");
        sb.append("    )\n}\n\n");

        copyAttr(sb);
        copyRef(sb);
    }

    private void buildMultiplicity(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Multiplicity changes: ").append(info.multiplicityChanges).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);

        sb.append("rule CopyEReference {\n");
        sb.append("    from s : Ecore!EReference\n");
        sb.append("    to   t : Ecore!EReference (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        eType       <- s.eType,\n");
        sb.append("        lowerBound  <- s.lowerBound,\n");
        sb.append("        upperBound  <- s.upperBound,\n");
        sb.append("        containment <- s.containment\n");
        sb.append("    )\n}\n");
    }

    private void buildContainment(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Containment changes: ").append(info.containmentChanges).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);

        sb.append("rule UpdateContainment {\n");
        sb.append("    from s : Ecore!EReference\n");
        sb.append("    to   t : Ecore!EReference (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        eType       <- s.eType,\n");
        sb.append("        containment <- not s.containment,\n");
        sb.append("        lowerBound  <- s.lowerBound,\n");
        sb.append("        upperBound  <- s.upperBound\n");
        sb.append("    )\n}\n");
    }

    private void buildAbstract(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Abstract changes: ").append(info.abstractChanges).append("\n\n");
        copyPackage(sb, false);

        sb.append("rule UpdateAbstract {\n");
        sb.append("    from s : Ecore!EClass\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name     <- s.name,\n");
        sb.append("        abstract <- not s.abstract,\n");
        sb.append("        eSuperTypes <- s.eSuperTypes,\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("    )\n}\n\n");

        copyAttr(sb);
        copyRef(sb);
    }

    private void buildSupertype(StringBuilder sb, DeltaInfo info) {
        sb.append("-- Supertype changes: ").append(info.supertypeChanges).append("\n\n");
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void buildMixed(StringBuilder sb, DeltaInfo info) {
        sb.append("helper def : classesToRemove : Set(String)        = ")
          .append(toSet(info.removedClasses)).append(";\n");
        sb.append("helper def : attrsToRemove   : Set(String)        = ")
          .append(toSet(info.removedAttrs)).append(";\n");
        sb.append("helper def : refsToRemove    : Set(String)        = ")
          .append(toSet(info.removedRefs)).append(";\n");
        sb.append("helper def : renameMap       : Map(String,String) = ")
          .append(toMap(info.renames)).append(";\n\n");

        sb.append("rule MixedEPackage {\n");
        sb.append("    from s : Ecore!EPackage\n");
        sb.append("    to   t : Ecore!EPackage (\n");
        sb.append("        name         <- s.name,\n");
        sb.append("        nsURI        <- s.nsURI,\n");
        sb.append("        nsPrefix     <- s.nsPrefix,\n");
        sb.append("        eClassifiers <- s.eClassifiers\n");
        sb.append("            ->select(c | not thisModule.classesToRemove->includes(c.name))\n");
        sb.append("            ->collect(c | thisModule.resolveTemp(c, 't'))\n");
        sb.append("    )\n}\n\n");

        sb.append("rule MixedEClass {\n");
        sb.append("    from s : Ecore!EClass (\n");
        sb.append("        not thisModule.classesToRemove->includes(s.name)\n");
        sb.append("    )\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        abstract    <- s.abstract,\n");
        sb.append("        eSuperTypes <- s.eSuperTypes\n");
        sb.append("            ->select(st | not thisModule.classesToRemove->includes(st.name)),\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("            ->reject(f | (f.oclIsKindOf(Ecore!EAttribute)\n");
        sb.append("                          and thisModule.attrsToRemove->includes(f.name))\n");
        sb.append("                      or (f.oclIsKindOf(Ecore!EReference)\n");
        sb.append("                          and thisModule.refsToRemove->includes(f.name)))\n");
        sb.append("            ->collect(f | thisModule.resolveTemp(f, 't'))\n");
        sb.append("    )\n}\n\n");

        sb.append("rule MixedEAttribute {\n");
        sb.append("    from s : Ecore!EAttribute (\n");
        sb.append("        not thisModule.attrsToRemove->includes(s.name)\n");
        sb.append("    )\n");
        sb.append("    to   t : Ecore!EAttribute (\n");
        sb.append("        name <- if thisModule.renameMap->includesKey(s.name)\n");
        sb.append("                then thisModule.renameMap->get(s.name)\n");
        sb.append("                else s.name endif,\n");
        sb.append("        eType      <- s.eType,\n");
        sb.append("        lowerBound <- s.lowerBound,\n");
        sb.append("        upperBound <- s.upperBound,\n");
        sb.append("        changeable <- s.changeable,\n");
        sb.append("        derived    <- s.derived\n");
        sb.append("    )\n}\n\n");

        sb.append("rule MixedEReference {\n");
        sb.append("    from s : Ecore!EReference (\n");
        sb.append("        not thisModule.refsToRemove->includes(s.name)\n");
        sb.append("        and not thisModule.classesToRemove->includes(s.eType.name)\n");
        sb.append("    )\n");
        sb.append("    to   t : Ecore!EReference (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        eType       <- s.eType,\n");
        sb.append("        lowerBound  <- s.lowerBound,\n");
        sb.append("        upperBound  <- s.upperBound,\n");
        sb.append("        containment <- s.containment\n");
        sb.append("    )\n}\n");
    }

    private void buildIdentity(StringBuilder sb) {
        copyPackage(sb, false);
        copyClass(sb);
        copyAttr(sb);
        copyRef(sb);
    }

    private void copyPackage(StringBuilder sb, boolean filtered) {
        sb.append("rule CopyEPackage {\n");
        sb.append("    from s : Ecore!EPackage\n");
        sb.append("    to   t : Ecore!EPackage (\n");
        sb.append("        name         <- s.name,\n");
        sb.append("        nsURI        <- s.nsURI,\n");
        sb.append("        nsPrefix     <- s.nsPrefix,\n");
        if (filtered) {
            sb.append("        eClassifiers <- s.eClassifiers\n");
            sb.append("            ->select(c | not thisModule.classesToRemove->includes(c.name))\n");
        } else {
            sb.append("        eClassifiers <- s.eClassifiers\n");
        }
        sb.append("    )\n}\n\n");
    }

    private void copyClass(StringBuilder sb) {
        sb.append("rule CopyEClass {\n");
        sb.append("    from s : Ecore!EClass\n");
        sb.append("    to   t : Ecore!EClass (\n");
        sb.append("        name                <- s.name,\n");
        sb.append("        abstract            <- s.abstract,\n");
        sb.append("        eSuperTypes         <- s.eSuperTypes,\n");
        sb.append("        eStructuralFeatures <- s.eStructuralFeatures\n");
        sb.append("    )\n}\n\n");
    }

    private void copyAttr(StringBuilder sb) {
        sb.append("rule CopyEAttribute {\n");
        sb.append("    from s : Ecore!EAttribute\n");
        sb.append("    to   t : Ecore!EAttribute (\n");
        sb.append("        name       <- s.name,\n");
        sb.append("        eType      <- s.eType,\n");
        sb.append("        lowerBound <- s.lowerBound,\n");
        sb.append("        upperBound <- s.upperBound,\n");
        sb.append("        changeable <- s.changeable,\n");
        sb.append("        derived    <- s.derived\n");
        sb.append("    )\n}\n\n");
    }

    private void copyRef(StringBuilder sb) {
        sb.append("rule CopyEReference {\n");
        sb.append("    from s : Ecore!EReference\n");
        sb.append("    to   t : Ecore!EReference (\n");
        sb.append("        name        <- s.name,\n");
        sb.append("        eType       <- s.eType,\n");
        sb.append("        lowerBound  <- s.lowerBound,\n");
        sb.append("        upperBound  <- s.upperBound,\n");
        sb.append("        containment <- s.containment\n");
        sb.append("    )\n}\n");
    }

    private DeltaInfo parseDeltaInfo(List<String> deltas) {
        DeltaInfo info = new DeltaInfo();
        info.total = deltas.size();

        for (String raw : deltas) {
            String d = raw.trim();

            if (d.startsWith("ADD CLASS ")) {
                info.addedClasses.add(clean(d, "ADD CLASS "));
            } else if (d.startsWith("DELETE CLASS ") || d.startsWith("REMOVE CLASS ")) {
                info.removedClasses.add(
                        d.startsWith("DELETE CLASS ")
                                ? clean(d, "DELETE CLASS ")
                                : clean(d, "REMOVE CLASS ")
                );
            } else if (d.startsWith("ADD ATTRIBUTE ")) {
                info.addedAttrs.add(last(clean(d, "ADD ATTRIBUTE ")));
            } else if (d.startsWith("DELETE ATTRIBUTE ") || d.startsWith("REMOVE ATTRIBUTE ")) {
                info.removedAttrs.add(
                        last(
                                d.startsWith("DELETE ATTRIBUTE ")
                                        ? clean(d, "DELETE ATTRIBUTE ")
                                        : clean(d, "REMOVE ATTRIBUTE ")
                        )
                );
            } else if (d.startsWith("RENAME ATTRIBUTE ")) {
                String[] p = clean(d, "RENAME ATTRIBUTE ").split("\\s*->\\s*", 2);
                if (p.length == 2) {
                    info.renames.put(last(p[0].trim()), last(p[1].trim()));
                }
            } else if (d.startsWith("ADD REFERENCE ")) {
                info.addedRefs.add(last(clean(d, "ADD REFERENCE ")));
            } else if (d.startsWith("DELETE REFERENCE ") || d.startsWith("REMOVE REFERENCE ")) {
                info.removedRefs.add(
                        last(
                                d.startsWith("DELETE REFERENCE ")
                                        ? clean(d, "DELETE REFERENCE ")
                                        : clean(d, "REMOVE REFERENCE ")
                        )
                );
            } else if (d.startsWith("CHANGE REFERENCE ")) {
                String det = detail(d);
                String n = beforePipe(clean(d, "CHANGE REFERENCE "));
                if (det.startsWith("multiplicity")) {
                    info.multiplicityChanges.add(n);
                } else if (det.startsWith("containment")) {
                    info.containmentChanges.add(n);
                }
            } else if (d.startsWith("CHANGE CLASS ")) {
                String det = detail(d);
                String n = beforePipe(clean(d, "CHANGE CLASS "));
                if (det.startsWith("abstract")) {
                    info.abstractChanges.add(n);
                } else if (det.startsWith("supertypes")) {
                    info.supertypeChanges.add(n);
                }
            } else if (d.startsWith("CHANGE ATTRIBUTE ")) {
                String det = detail(d);
                String n = beforePipe(clean(d, "CHANGE ATTRIBUTE "));
                if (det.startsWith("type")) {
                    info.typeChanges.add(n);
                }
            }
        }

        return info;
    }

    private String clean(String s, String prefix) {
        return s.startsWith(prefix) ? s.substring(prefix.length()).trim() : s;
    }

    private String last(String s) {
        s = beforePipe(s);
        return s.contains("::")
                ? s.substring(s.lastIndexOf("::") + 2).trim()
                : s.trim();
    }

    private String beforePipe(String s) {
        return s.contains("|") ? s.substring(0, s.indexOf('|')).trim() : s.trim();
    }

    private String detail(String s) {
        return s.contains("|") ? s.substring(s.indexOf('|') + 1).trim() : "";
    }

    private String fname(String p) {
        return p != null ? new File(p).getName() : "?";
    }

    private String toSet(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "Set{}";
        }
        return "Set{" + items.stream()
                .map(i -> "'" + i + "'")
                .collect(Collectors.joining(", ")) + "}";
    }

    private String toMap(Map<String, String> m) {
        if (m == null || m.isEmpty()) {
            return "Map{}";
        }
        return "Map{" + m.entrySet().stream()
                .map(e -> "'" + e.getKey() + "' -> '" + e.getValue() + "'")
                .collect(Collectors.joining(", ")) + "}";
    }

    private static class DeltaInfo {
        int total = 0;
        List<String> addedClasses = new ArrayList<>();
        List<String> removedClasses = new ArrayList<>();
        List<String> addedAttrs = new ArrayList<>();
        List<String> removedAttrs = new ArrayList<>();
        List<String> addedRefs = new ArrayList<>();
        List<String> removedRefs = new ArrayList<>();
        List<String> typeChanges = new ArrayList<>();
        List<String> multiplicityChanges = new ArrayList<>();
        List<String> containmentChanges = new ArrayList<>();
        List<String> abstractChanges = new ArrayList<>();
        List<String> supertypeChanges = new ArrayList<>();
        Map<String, String> renames = new LinkedHashMap<>();
    }

    public List<String> listAtlFiles() {
        List<String> list = new ArrayList<>();
        File dir = new File(transformationsDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".atl"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) {
                    list.add(f.getName());
                }
            }
        }
        return list;
    }

    public String getTransformationsDir() {
        return transformationsDir;
    }

    public static class MigrationPlan {

        private String sourceModel;
        private String targetModel;
        private String outputModel;
        private String atlFile;
        private String atlPath;
        private String predictionLabel;
        private double confidence;
        private long timestamp;
        private List<String> actions = new ArrayList<>();

        public String getSourceModel() {
            return sourceModel;
        }

        public void setSourceModel(String v) {
            sourceModel = v;
        }

        public String getTargetModel() {
            return targetModel;
        }

        public void setTargetModel(String v) {
            targetModel = v;
        }

        public String getOutputModel() {
            return outputModel;
        }

        public void setOutputModel(String v) {
            outputModel = v;
        }

        public String getAtlFile() {
            return atlFile;
        }

        public void setAtlFile(String v) {
            atlFile = v;
        }

        public String getAtlPath() {
            return atlPath;
        }

        public void setAtlPath(String v) {
            atlPath = v;
        }

        public String getPredictionLabel() {
            return predictionLabel;
        }

        public void setPredictionLabel(String v) {
            predictionLabel = v;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double v) {
            confidence = v;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long v) {
            timestamp = v;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> v) {
            actions = v;
        }

        public void addAction(String a) {
            actions.add(a);
        }

        @Override
        public String toString() {
            return String.format(
                    "MigrationPlan{src=%s, atl=%s, label=%s, conf=%.1f%%, deltas=%d}",
                    sourceModel != null ? new File(sourceModel).getName() : "null",
                    atlFile,
                    predictionLabel,
                    confidence * 100,
                    actions.size()
            );
        }
    }
}