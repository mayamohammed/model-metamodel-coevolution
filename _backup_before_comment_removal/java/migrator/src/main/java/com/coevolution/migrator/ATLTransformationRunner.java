package com.coevolution.migrator;

import com.coevolution.migrator.TransformationGenerator.MigrationPlan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class ATLTransformationRunner {

    public enum RunMode { SIMULATE, ECLIPSE_ATL }

    private final RunMode mode;
    private final String outputDir;

    private String v2Content = "";

    public ATLTransformationRunner(String outputDir) {
        this.outputDir = outputDir;
        this.mode = RunMode.SIMULATE;
        new File(outputDir).mkdirs();
    }

    public ATLTransformationRunner(String outputDir, RunMode mode) {
        this.outputDir = outputDir;
        this.mode = mode;
        new File(outputDir).mkdirs();
    }

    // ============================================================
    // RUN
    // ============================================================
    public MigrationResult run(MigrationPlan plan) {
    	
        long start = System.currentTimeMillis();

        System.out.println("[ATL-RUN] sourceModel = " + plan.getSourceModel());
        System.out.println("[ATL-RUN] src exists  = " + new File(plan.getSourceModel()).exists());
        System.out.println("[ATL-RUN] atlPath     = " + plan.getAtlPath());

        System.out.printf("[ATL] Running %s on %s ...%n",
                plan.getAtlFile(), new File(plan.getSourceModel()).getName());

        MigrationResult result = new MigrationResult();
        result.setPlan(plan);

        try {
            if (mode == RunMode.SIMULATE) {
                simulateMigration(plan, result);
            } else {
                runEclipseAtl(plan, result);
            }

            boolean ok = result.getOutputPath() != null
                    && !result.getOutputPath().isBlank()
                    && new File(result.getOutputPath()).exists();

            result.setSuccess(ok);
            if (!ok) {
                result.setError("Output file not created: " + result.getOutputPath());
                System.err.println("[ATL] Error: " + result.getError());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError(e.getMessage());
            System.err.println("[ATL] Error: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        System.out.printf("[ATL] %s  (%d ms)%n",
                result.isSuccess() ? "OK" : "FAILED", result.getDurationMs());
        return result;
    }

    public List<MigrationResult> runBatch(List<MigrationPlan> plans) {
        List<MigrationResult> results = new ArrayList<>();
        int ok = 0, fail = 0;
        System.out.printf("[ATL] Batch: %d plans%n", plans.size());
        for (int i = 0; i < plans.size(); i++) {
            System.out.printf("[ATL] [%d/%d] ", i + 1, plans.size());
            MigrationResult r = run(plans.get(i));
            results.add(r);
            if (r.isSuccess()) ok++; else fail++;
        }
        System.out.printf("[ATL] Batch done: %d OK / %d FAILED%n", ok, fail);
        return results;
    }

    // ============================================================
    // SIMULATE
    // ============================================================
    private void simulateMigration(MigrationPlan plan, MigrationResult result) throws IOException {
        File src = new File(plan.getSourceModel());
        if (!src.exists())
            throw new FileNotFoundException("Source not found: " + plan.getSourceModel());

        String content = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);

        v2Content = "";
        if (plan.getTargetModel() != null && !plan.getTargetModel().isBlank()) {
            File v2f = new File(plan.getTargetModel());
            if (v2f.exists()) {
                v2Content = new String(Files.readAllBytes(v2f.toPath()), StandardCharsets.UTF_8);
                System.out.printf("[ATL] v2Content loaded: %d chars from %s%n",
                        v2Content.length(), v2f.getName());
            } else {
                System.out.println("[ATL] v2Content: file not found → " + plan.getTargetModel());
            }
        } else {
            System.out.println("[ATL] v2Content: targetModel null/blank — fallback mode");
        }

        String transformed = applyActions(content, plan);

        String outFile = outputDir + File.separator
                + src.getName().replace(".ecore", "_migrated.ecore");
        Files.write(
                Paths.get(outFile),
                transformed.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        result.setOutputPath(outFile);
        result.setTransformationType("SIMULATE");
        result.setLinesIn(content.split("\\n").length);
        result.setLinesOut(transformed.split("\\n").length);

        System.out.printf("[ATL] Simulated: %d -> %d lines  out=%s%n",
                result.getLinesIn(), result.getLinesOut(), new File(outFile).getName());
    }

    // ============================================================
    // APPLY ACTIONS
    // ============================================================
    private String applyActions(String content, MigrationPlan plan) {
        String transformed = content;

        List<String> actions = new ArrayList<>(
                plan.getActions() != null ? plan.getActions() : Collections.emptyList());
        actions.sort((a, b) -> actionPriority(a) - actionPriority(b));

        System.out.printf("[ATL] Applying %d actions (sorted)...%n", actions.size());

        Set<String> classesAddedFromV2 = new HashSet<>();
        for (String action : actions) {
            if (action == null) continue;
            if (action.startsWith("ADD CLASS")) {
                Matcher m = Pattern.compile("ADD CLASS\\s+([A-Za-z0-9_]+)").matcher(action);
                if (m.find()) classesAddedFromV2.add(m.group(1));
            }
        }

        for (String action : actions) {
            if (action == null || action.isBlank()) continue;
            System.out.println("[ATL] Action: " + action);

            if (action.startsWith("CHANGE PACKAGE"))
                transformed = applyPackageNsUriChange(transformed, action);

            else if (action.startsWith("DELETE CLASS"))
                transformed = applyDeleteClass(transformed, action);

            else if (action.startsWith("DELETE ATTRIBUTE"))
                transformed = applyDeleteAttribute(transformed, action);

            else if (action.startsWith("DELETE REFERENCE"))
                transformed = applyDeleteReference(transformed, action);

            else if (action.startsWith("RENAME ATTRIBUTE") || action.contains("rename:"))
                transformed = applyRenameAttribute(transformed, action);

            else if (action.startsWith("ADD CLASS"))
                transformed = applyAddClass(transformed, action);

            else if (action.startsWith("ADD ATTRIBUTE")) {
                String ownerClass = extractClassName(action, "ADD ATTRIBUTE");
                if (ownerClass != null && classesAddedFromV2.contains(ownerClass)) {
                    System.out.printf("[ATL]   ADD ATTRIBUTE %s — class copied from V2, skipped%n", action);
                    continue;
                }
                transformed = applyAddAttribute(transformed, action);
            }

            else if (action.startsWith("ADD REFERENCE")) {
                String ownerClass = extractClassName(action, "ADD REFERENCE");
                if (ownerClass != null && classesAddedFromV2.contains(ownerClass)) {
                    System.out.printf("[ATL]   ADD REFERENCE %s — class copied from V2, skipped%n", action);
                    continue;
                }
                transformed = applyAddReference(transformed, action);
            }

            else if (action.startsWith("CHANGE ATTRIBUTE"))
                transformed = applyChangeAttribute(transformed, action);

            else if (action.startsWith("CHANGE CLASS"))
                transformed = applyChangeClass(transformed, action);

            else if (action.startsWith("CHANGE REFERENCE")) {
                if (action.contains("containment:"))
                    transformed = applyReferenceContainmentChange(transformed, action);
                else
                    transformed = applyReferenceMultiplicityChange(transformed, action);
            }
        }

        String comment = String.format(
                "%n  <!-- ATL Migration Applied | label=%s | ATL=%s | actions=%d -->%n",
                plan.getPredictionLabel(), plan.getAtlFile(), actions.size());
        transformed = transformed.replace("</ecore:EPackage>", comment + "</ecore:EPackage>");
        return transformed;
    }

    private int actionPriority(String action) {
        if (action == null) return 99;
        if (action.startsWith("CHANGE PACKAGE"))   return 0;
        if (action.startsWith("DELETE CLASS"))     return 1;
        if (action.startsWith("DELETE ATTRIBUTE")) return 2;
        if (action.startsWith("DELETE REFERENCE")) return 3;
        if (action.startsWith("RENAME ATTRIBUTE")) return 4;
        if (action.startsWith("ADD CLASS"))        return 5;
        if (action.startsWith("ADD ATTRIBUTE"))    return 6;
        if (action.startsWith("ADD REFERENCE"))    return 7;
        if (action.startsWith("CHANGE ATTRIBUTE")) return 8;
        if (action.startsWith("CHANGE CLASS"))     return 9;
        if (action.startsWith("CHANGE REFERENCE")) return 10;
        return 11;
    }

    private String extractClassName(String action, String prefix) {
        Pattern p = Pattern.compile(
                Pattern.quote(prefix) + "\\s+([A-Za-z0-9_]+)::[A-Za-z0-9_]+");
        Matcher m = p.matcher(action);
        return m.find() ? m.group(1) : null;
    }

    // ============================================================
    // ACTION : CHANGE PACKAGE nsURI
    // ============================================================
    private String applyPackageNsUriChange(String content, String action) {
        Pattern p = Pattern.compile("nsURI:\\s*(\\S+)\\s*->\\s*(\\S+)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String oldUri = m.group(1).trim();
            String newUri = m.group(2).trim();
            System.out.printf("[ATL]   nsURI: %s -> %s%n", oldUri, newUri);
            return content.replace("nsURI=\"" + oldUri + "\"",
                                   "nsURI=\"" + newUri + "\"");
        }
        return content;
    }

    // ============================================================
    // ACTION : ADD CLASS
    // ============================================================
    private String applyAddClass(String content, String action) {
        Pattern p = Pattern.compile("ADD CLASS\\s+([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (!m.find()) return content;

        String className = m.group(1);

        if (containsClass(content, className)) {
            System.out.printf("[ATL]   ADD CLASS %s — already exists, skipped%n", className);
            return content;
        }

        String newClassBlock = buildClassBlockFromV2(className);
        if (newClassBlock == null) {
            newClassBlock =
                "\n  <eClassifiers xsi:type=\"ecore:EClass\" name=\"" + className + "\">\n" +
                "  </eClassifiers>\n";
            System.out.printf("[ATL]   ADD CLASS %s (empty fallback)%n", className);
        } else {
            System.out.printf("[ATL]   ADD CLASS %s (copied from V2)%n", className);
        }

        return content.replace("</ecore:EPackage>", newClassBlock + "</ecore:EPackage>");
    }

    private String buildClassBlockFromV2(String className) {
        if (v2Content == null || v2Content.isBlank()) return null;
        String block = extractEClassBlock(v2Content, className);
        if (block == null) return null;
        return "\n  " + block.trim() + "\n";
    }

    // ============================================================
    // ACTION : DELETE CLASS
    // ============================================================
    private String applyDeleteClass(String content, String action) {
        Pattern p = Pattern.compile("DELETE CLASS\\s+([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className = m.group(1);
            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                System.out.printf("[ATL]   DELETE CLASS %s%n", className);
                return content.replace(classBlock, "");
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : DELETE ATTRIBUTE
    // ============================================================
    private String applyDeleteAttribute(String content, String action) {
        Pattern p = Pattern.compile(
                "DELETE ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className  = m.group(1);
            String attrName   = m.group(2);
            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                System.out.printf("[ATL]   DELETE ATTRIBUTE %s::%s%n", className, attrName);
                String updatedBlock = removeStructuralFeature(classBlock, attrName);
                return content.replace(classBlock, updatedBlock);
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : DELETE REFERENCE
    // ============================================================
    private String applyDeleteReference(String content, String action) {
        Pattern p = Pattern.compile(
                "DELETE REFERENCE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className = m.group(1);
            String refName   = m.group(2);
            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                System.out.printf("[ATL]   DELETE REFERENCE %s::%s%n", className, refName);
                String updatedBlock = removeStructuralFeature(classBlock, refName);
                return content.replace(classBlock, updatedBlock);
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : ADD ATTRIBUTE
    // ============================================================
    private String applyAddAttribute(String content, String action) {
        Pattern p = Pattern.compile(
                "ADD ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className  = m.group(1);
            String attrName   = m.group(2);
            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                if (classBlock.contains("name=\"" + attrName + "\"")) {
                    System.out.printf("[ATL]   ADD ATTRIBUTE %s::%s — already exists, skipped%n",
                            className, attrName);
                    return content;
                }
                String eType = resolveAttrTypeFromV2(className, attrName);
                String newAttribute =
                        "    <eStructuralFeatures xsi:type=\"ecore:EAttribute\"\n" +
                        "        name=\"" + attrName + "\"\n" +
                        "        eType=\"" + eType + "\"/>\n";
                System.out.printf("[ATL]   ADD ATTRIBUTE %s::%s (type=%s)%n",
                        className, attrName, eType);
                String updatedBlock = classBlock.replace("</eClassifiers>",
                        newAttribute + "  </eClassifiers>");
                return content.replace(classBlock, updatedBlock);
            } else {
                System.out.printf("[ATL]   ADD ATTRIBUTE %s::%s — class not found, skipped%n",
                        className, attrName);
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : ADD REFERENCE
    // ============================================================
    private String applyAddReference(String content, String action) {
        Pattern p = Pattern.compile(
                "ADD REFERENCE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);
        if (!m.find()) return content;

        String className = m.group(1);
        String refName   = m.group(2);

        String classBlock = extractEClassBlock(content, className);
        if (classBlock == null) {
            System.out.printf("[ATL]   ADD REFERENCE %s::%s — class not found, skipped%n",
                    className, refName);
            return content;
        }

        if (classBlock.contains("name=\"" + refName + "\"")) {
            System.out.printf("[ATL]   ADD REFERENCE %s::%s — already exists, skipped%n",
                    className, refName);
            return content;
        }

        String newRef = resolveReferenceFromV2(className, refName);
        if (newRef == null) {
            newRef = "    <eStructuralFeatures xsi:type=\"ecore:EReference\"\n" +
                     "        name=\"" + refName + "\"\n" +
                     "        containment=\"true\"/>\n";
            System.out.printf("[ATL]   ADD REFERENCE %s::%s (fallback)%n", className, refName);
        } else {
            System.out.printf("[ATL]   ADD REFERENCE %s::%s (copied from V2)%n", className, refName);
        }

        String updatedBlock = classBlock.replace("</eClassifiers>",
                newRef + "  </eClassifiers>");
        return content.replace(classBlock, updatedBlock);
    }

    private String resolveReferenceFromV2(String className, String refName) {
        if (v2Content == null || v2Content.isBlank()) return null;
        String v2ClassBlock = extractEClassBlock(v2Content, className);
        if (v2ClassBlock == null) return null;

        Pattern tagPat = Pattern.compile("<eStructuralFeatures[\\s\\S]*?/>", Pattern.DOTALL);
        Matcher tagM = tagPat.matcher(v2ClassBlock);
        while (tagM.find()) {
            String tag = tagM.group(0);
            if (tag.contains("name=\"" + refName + "\"") && tag.contains("EReference"))
                return "    " + tag.trim() + "\n";
        }
        return null;
    }

    // ============================================================
    // ACTION : CHANGE ATTRIBUTE (type)
    // ============================================================
    private String applyChangeAttribute(String content, String action) {
        Pattern p = Pattern.compile(
                "CHANGE ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)" +
                ".*type:\\s*(\\S+)\\s*->\\s*(\\S+)");
        Matcher m = p.matcher(action);
        if (!m.find()) return content;

        String className   = m.group(1);
        String attrName    = m.group(2);
        String newEType    = toEcoreType(m.group(4).trim());

        String classBlock = extractEClassBlock(content, className);
        if (classBlock == null) {
            System.out.printf("[ATL]   CHANGE ATTRIBUTE %s::%s — class not found%n",
                    className, attrName);
            return content;
        }

        Pattern tagPat = Pattern.compile("<eStructuralFeatures[\\s\\S]*?/>", Pattern.DOTALL);
        Matcher tagM = tagPat.matcher(classBlock);
        StringBuffer sb = new StringBuffer();
        boolean found = false;

        while (tagM.find()) {
            String tag = tagM.group(0);
            if (tag.contains("name=\"" + attrName + "\"")) {
                String updatedTag;
                if (tag.contains("eType=\"")) {
                    updatedTag = tag.replaceAll(
                            "eType=\"[^\"]+\"",
                            "eType=\"" + Matcher.quoteReplacement(newEType) + "\"");
                } else {
                    updatedTag = tag.replace("/>",
                            " eType=\"" + newEType + "\"/>");
                }
                tagM.appendReplacement(sb, Matcher.quoteReplacement(updatedTag));
                found = true;
                System.out.printf("[ATL]   CHANGE ATTRIBUTE %s::%s eType -> %s%n",
                        className, attrName, newEType);
            } else {
                tagM.appendReplacement(sb, Matcher.quoteReplacement(tag));
            }
        }
        tagM.appendTail(sb);

        if (!found) {
            System.out.printf("[ATL]   CHANGE ATTRIBUTE %s::%s — attr not found%n",
                    className, attrName);
            return content;
        }
        return content.replace(classBlock, sb.toString());
    }

    private String toEcoreType(String shortName) {
        String base = "ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//";
        switch (shortName) {
            case "EString":  return base + "EString";
            case "EInt":     return base + "EInt";
            case "EDouble":  return base + "EDouble";
            case "EFloat":   return base + "EFloat";
            case "EBoolean": return base + "EBoolean";
            case "ELong":    return base + "ELong";
            case "EByte":    return base + "EByte";
            case "EChar":    return base + "EChar";
            default:
                return shortName.contains("://") ? shortName : base + shortName;
        }
    }

    private String resolveAttrTypeFromV2(String className, String attrName) {
        String fallback =
                "ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString";
        if (v2Content == null || v2Content.isBlank()) return fallback;

        String v2ClassBlock = extractEClassBlock(v2Content, className);
        if (v2ClassBlock == null) return fallback;

        Pattern tagPat = Pattern.compile("<eStructuralFeatures[\\s\\S]*?/>", Pattern.DOTALL);
        Matcher tagM = tagPat.matcher(v2ClassBlock);
        while (tagM.find()) {
            String tag = tagM.group(0);
            if (tag.contains("name=\"" + attrName + "\"")) {
                Matcher et = Pattern.compile("eType=\"([^\"]+)\"").matcher(tag);
                if (et.find()) {
                    String resolved = et.group(1);
                    System.out.printf("[ATL]   resolveAttrType %s::%s -> %s%n",
                            className, attrName, resolved);
                    return resolved;
                }
            }
        }
        System.out.printf("[ATL]   resolveAttrType %s::%s -> fallback EString%n",
                className, attrName);
        return fallback;
    }

    // ============================================================
    // ACTION : RENAME ATTRIBUTE
    // ============================================================
    private String applyRenameAttribute(String content, String action) {
        Pattern p = Pattern.compile(
                "RENAME ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)" +
                "\\s*->\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);

        if (!m.find()) {
            p = Pattern.compile(
                    "rename:\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)" +
                    "\\s*->\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
            m = p.matcher(action);
            if (!m.find()) return content;
        }

        String oldClass = m.group(1);
        String oldAttr  = m.group(2);
        String newClass = m.group(3);
        String newAttr  = m.group(4);

        if (oldClass.equals(newClass)) {
            String classBlock = extractEClassBlock(content, oldClass);
            if (classBlock != null) {
                System.out.printf("[ATL]   RENAME %s::%s -> %s::%s%n",
                        oldClass, oldAttr, newClass, newAttr);
                String updatedBlock = classBlock.replaceAll(
                        "(<eStructuralFeatures[^>]*name=\")"
                                + Pattern.quote(oldAttr) + "\"",
                        "$1" + newAttr + "\"");
                return content.replace(classBlock, updatedBlock);
            }
        } else {
            String srcBlock = extractEClassBlock(content, oldClass);
            String dstBlock = extractEClassBlock(content, newClass);
            if (srcBlock != null && dstBlock != null) {
                System.out.printf("[ATL]   RENAME+MOVE %s::%s -> %s::%s%n",
                        oldClass, oldAttr, newClass, newAttr);
                Pattern attrPat = Pattern.compile(
                        "\\s*<eStructuralFeatures\\b[^>]*\\bname=\""
                                + Pattern.quote(oldAttr) + "\"[\\s\\S]*?/>");
                Matcher am = attrPat.matcher(srcBlock);
                String movedAttr;
                if (am.find()) {
                    movedAttr = am.group(0).replaceAll(
                            "(name=\")" + Pattern.quote(oldAttr) + "\"",
                            "$1" + newAttr + "\"");
                } else {
                    movedAttr =
                        "\n    <eStructuralFeatures xsi:type=\"ecore:EAttribute\"\n" +
                        "        name=\"" + newAttr + "\"\n" +
                        "        eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>\n";
                }
                String updatedSrc = removeStructuralFeature(srcBlock, oldAttr);
                String tmp = content.replace(srcBlock, updatedSrc);
                String dstBlockFresh = extractEClassBlock(tmp, newClass);
                if (dstBlockFresh != null) {
                    String updatedDst = dstBlockFresh.replace(
                            "</eClassifiers>",
                            movedAttr + "  </eClassifiers>");
                    return tmp.replace(dstBlockFresh, updatedDst);
                }
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : CHANGE CLASS (supertypes + abstract)
    // ============================================================
    private String applyChangeClass(String content, String action) {

        Pattern pSuper = Pattern.compile(
                "CHANGE CLASS\\s+([A-Za-z0-9_]+)[^|]*\\|[^|]*supertypes:\\s*\\[[^\\]]*\\]" +
                "\\s*->\\s*\\[([^\\]]*)\\]",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher mSuper = pSuper.matcher(action);
        if (mSuper.find()) {
            String className = mSuper.group(1).trim();
            String newSupers = mSuper.group(2).trim();
            if (!newSupers.isBlank()) {
                String classBlock = extractEClassBlock(content, className);
                if (classBlock != null) {
                    StringBuilder superRef = new StringBuilder();
                    for (String s : newSupers.split(",")) {
                        String t = s.trim();
                        if (!t.isEmpty()) {
                            if (superRef.length() > 0) superRef.append(" ");
                            superRef.append("#//").append(t);
                        }
                    }
                    String updated;
                    if (classBlock.contains("eSuperTypes=")) {
                        updated = classBlock.replaceFirst(
                                "eSuperTypes=\"[^\"]*\"",
                                "eSuperTypes=\"" + superRef + "\"");
                    } else {
                        updated = classBlock.replaceFirst(
                                "(<eClassifiers\\b[^>]*\\bname=\""
                                        + Pattern.quote(className) + "\")",
                                "$1 eSuperTypes=\"" + superRef + "\"");
                    }
                    System.out.printf("[ATL]   CHANGE CLASS %s eSuperTypes -> %s%n",
                            className, superRef);
                    content = content.replace(classBlock, updated);
                }
            }
        }

        Pattern pAbs = Pattern.compile(
                "CHANGE CLASS\\s+([A-Za-z0-9_]+)[^|]*\\|[^|]*abstract:\\s*(true|false)" +
                "\\s*->\\s*(true|false)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher mAbs = pAbs.matcher(action);
        if (mAbs.find()) {
            String className   = mAbs.group(1).trim();
            String newAbstract = mAbs.group(3);
            String classBlock  = extractEClassBlock(content, className);
            if (classBlock != null) {
                String updated;
                if (classBlock.contains("abstract=")) {
                    updated = classBlock.replaceAll(
                            "abstract=\"(true|false)\"",
                            "abstract=\"" + newAbstract + "\"");
                } else {
                    updated = classBlock.replaceFirst(
                            "(<eClassifiers\\b[^>]*\\bname=\""
                                    + Pattern.quote(className) + "\")",
                            "$1 abstract=\"" + newAbstract + "\"");
                }
                System.out.printf("[ATL]   CHANGE CLASS %s abstract -> %s%n",
                        className, newAbstract);
                content = content.replace(classBlock, updated);
            }
        }

        return content;
    }

    // ============================================================
    // ACTION : CHANGE REFERENCE multiplicity
    // ============================================================
    private String applyReferenceMultiplicityChange(String content, String action) {
        Pattern p = Pattern.compile(
                "CHANGE REFERENCE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)" +
                ".*\\[(-?\\d+),(-?\\d+)\\]\\s*->\\s*\\[(-?\\d+),(-?\\d+)\\]");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className = m.group(1);
            String refName   = m.group(2);
            String newLower  = m.group(5);
            String newUpper  = m.group(6);

            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                Pattern refPat = Pattern.compile(
                        "<eStructuralFeatures\\s+xsi:type=\"ecore:EReference\"" +
                        "[\\s\\S]*?name=\"" + Pattern.quote(refName) + "\"[\\s\\S]*?/>",
                        Pattern.DOTALL);
                Matcher refM = refPat.matcher(classBlock);
                if (refM.find()) {
                    String refBlock   = refM.group(0);
                    String updatedRef = refBlock;
                    if (updatedRef.contains("upperBound=\"")) {
                        updatedRef = updatedRef.replaceAll(
                                "upperBound=\"-?\\d+\"",
                                "upperBound=\"" + newUpper + "\"");
                    } else {
                        updatedRef = updatedRef.replace("/>",
                                " upperBound=\"" + newUpper + "\"/>");
                    }
                    if (updatedRef.contains("lowerBound=\"")) {
                        updatedRef = updatedRef.replaceAll(
                                "lowerBound=\"-?\\d+\"",
                                "lowerBound=\"" + newLower + "\"");
                    } else if (!"0".equals(newLower)) {
                        updatedRef = updatedRef.replace("/>",
                                " lowerBound=\"" + newLower + "\"/>");
                    }
                    System.out.printf("[ATL]   CHANGE REFERENCE %s::%s upper=%s lower=%s%n",
                            className, refName, newUpper, newLower);
                    return content.replace(classBlock,
                            classBlock.replace(refBlock, updatedRef));
                }
            }
        }
        return content;
    }

    // ============================================================
    // ACTION : CHANGE REFERENCE containment
    // ============================================================
    private String applyReferenceContainmentChange(String content, String action) {
        Pattern p = Pattern.compile(
                "CHANGE REFERENCE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)" +
                ".*containment:\\s*(true|false)\\s*->\\s*(true|false)");
        Matcher m = p.matcher(action);
        if (m.find()) {
            String className      = m.group(1);
            String refName        = m.group(2);
            String newContainment = m.group(4);

            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null) {
                Pattern refPat = Pattern.compile(
                        "<eStructuralFeatures\\s+xsi:type=\"ecore:EReference\"" +
                        "[\\s\\S]*?name=\"" + Pattern.quote(refName) + "\"[\\s\\S]*?/>",
                        Pattern.DOTALL);
                Matcher refM = refPat.matcher(classBlock);
                if (refM.find()) {
                    String refBlock   = refM.group(0);
                    String updatedRef;
                    if (refBlock.contains("containment=")) {
                        updatedRef = refBlock.replaceAll(
                                "containment=\"(true|false)\"",
                                "containment=\"" + newContainment + "\"");
                    } else {
                        updatedRef = refBlock.replace("/>",
                                " containment=\"" + newContainment + "\"/>");
                    }
                    System.out.printf("[ATL]   CHANGE REFERENCE %s::%s containment -> %s%n",
                            className, refName, newContainment);
                    return content.replace(classBlock,
                            classBlock.replace(refBlock, updatedRef));
                }
            }
        }
        return content;
    }

    // ============================================================
    // HELPERS XML
    // ============================================================
    private boolean containsClass(String content, String className) {
        Pattern p = Pattern.compile(
                "<eClassifiers\\b[^>]*\\bname=\"" + Pattern.quote(className) + "\"");
        return p.matcher(content).find();
    }

    private String extractEClassBlock(String content, String className) {
        Pattern p = Pattern.compile(
                "<eClassifiers\\b[^>]*\\bname=\"" + Pattern.quote(className) + "\"[^>]*>" +
                "[\\s\\S]*?</eClassifiers>",
                Pattern.DOTALL);
        Matcher m = p.matcher(content);
        if (m.find()) return m.group(0);

        Pattern pSelf = Pattern.compile(
                "<eClassifiers\\b[^>]*\\bname=\"" + Pattern.quote(className) + "\"[^>]*/>");
        Matcher mSelf = pSelf.matcher(content);
        if (mSelf.find()) return mSelf.group(0);

        return null;
    }

    private String removeStructuralFeature(String classBlock, String featureName) {
        Pattern p1 = Pattern.compile(
                "\\s*<eStructuralFeatures\\s+xsi:type=\"ecore:EAttribute\"" +
                "[\\s\\S]*?name=\"" + Pattern.quote(featureName) + "\"[\\s\\S]*?/>\\s*",
                Pattern.DOTALL);
        Matcher m1 = p1.matcher(classBlock);
        if (m1.find()) return m1.replaceFirst("\n");

        Pattern p2 = Pattern.compile(
                "\\s*<eStructuralFeatures\\b[^>]*\\bname=\""
                + Pattern.quote(featureName) + "\"[\\s\\S]*?/>\\s*",
                Pattern.DOTALL);
        Matcher m2 = p2.matcher(classBlock);
        if (m2.find()) return m2.replaceFirst("\n");

        return classBlock;
    }

    // ============================================================
    // ECLIPSE ATL
    // ============================================================
    private void runEclipseAtl(MigrationPlan plan, MigrationResult result) throws Exception {
        throw new UnsupportedOperationException(
                "Eclipse ATL runtime not configured. Use SIMULATE mode.");
    }

    // ============================================================
    // MigrationResult DTO
    // ============================================================
    public static class MigrationResult {
        private MigrationPlan plan;
        private boolean success;
        private String error;
        private String outputPath;
        private String transformationType;
        private int linesIn;
        private int linesOut;
        private long durationMs;

        public MigrationPlan getPlan()              { return plan; }
        public void setPlan(MigrationPlan v)        { this.plan = v; }
        public boolean isSuccess()                  { return success; }
        public void setSuccess(boolean v)           { this.success = v; }
        public String getError()                    { return error; }
        public void setError(String v)              { this.error = v; }
        public String getOutputPath()               { return outputPath; }
        public void setOutputPath(String v)         { this.outputPath = v; }
        public String getTransformationType()       { return transformationType; }
        public void setTransformationType(String v) { this.transformationType = v; }
        public int getLinesIn()                     { return linesIn; }
        public void setLinesIn(int v)               { this.linesIn = v; }
        public int getLinesOut()                    { return linesOut; }
        public void setLinesOut(int v)              { this.linesOut = v; }
        public long getDurationMs()                 { return durationMs; }
        public void setDurationMs(long v)           { this.durationMs = v; }

        @Override
        public String toString() {
            return String.format(
                    "MigrationResult{success=%b, atl=%s, lines=%d->%d, ms=%d}",
                    success, plan != null ? plan.getAtlFile() : "?",
                    linesIn, linesOut, durationMs);
        }
    }
}
