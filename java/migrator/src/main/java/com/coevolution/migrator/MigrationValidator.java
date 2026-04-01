package com.coevolution.migrator;

import com.coevolution.migrator.ATLTransformationRunner.MigrationResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationValidator {

    public ValidationResult validate(MigrationResult result) {
        ValidationResult vr = new ValidationResult();
        vr.setMigrationResult(result);

        if (!result.isSuccess()) {
            vr.setValid(false);
            vr.addError("Migration failed: " + result.getError());
            return vr;
        }

        String outPath = result.getOutputPath();
        if (outPath == null || !new File(outPath).exists()) {
            vr.setValid(false);
            vr.addError("Output file not found: " + outPath);
            return vr;
        }

        try {
            String content = new String(
                    Files.readAllBytes(Paths.get(outPath)),
                    StandardCharsets.UTF_8
            );

            validateContent(content, vr, result);

        } catch (IOException e) {
            vr.setValid(false);
            vr.addError("Cannot read output file: " + e.getMessage());
        }

        vr.setValid(vr.getErrors().isEmpty());
        return vr;
    }

    public List<ValidationResult> validateBatch(List<MigrationResult> results) {
        List<ValidationResult> list = new ArrayList<>();
        int ok = 0;
        int fail = 0;

        for (MigrationResult r : results) {
            ValidationResult vr = validate(r);
            list.add(vr);

            if (vr.isValid()) ok++;
            else fail++;
        }

        System.out.printf("[VAL] Batch: %d valid / %d invalid%n", ok, fail);
        return list;
    }

    private void validateContent(String content,
                                 ValidationResult vr,
                                 MigrationResult result) {

        if (!content.contains("<ecore:EPackage")) {
            vr.addError("Missing <ecore:EPackage> root element");
        }

        if (content.trim().isEmpty()) {
            vr.addError("Output file is empty");
        }

        if (result.getLinesOut() <= 0) {
            vr.addError("Invalid line count in output");
        }

        if (!content.contains("ATL Migration Applied")) {
            vr.addWarning("Migration comment not found");
        }

        if (result.getPlan() != null) {
            validateActions(content, result.getPlan().getActions(), vr);
        }
    }

    private void validateActions(String content,
                                 List<String> actions,
                                 ValidationResult vr) {

        if (actions == null || actions.isEmpty()) {
            vr.addWarning("No migration actions found in plan");
            return;
        }

        for (String action : actions) {
            if (action == null) continue;

            if (action.startsWith("CHANGE PACKAGE")) {
                validatePackageNsUri(content, action, vr);
            } else if (action.startsWith("RENAME ATTRIBUTE") || action.contains("rename:")) {
                validateRenameAttribute(content, action, vr);
            } else if (action.startsWith("DELETE ATTRIBUTE")) {
                validateDeleteAttribute(content, action, vr);
            } else if (action.startsWith("ADD ATTRIBUTE")) {
                validateAddAttribute(content, action, vr);
            } else if (action.startsWith("CHANGE REFERENCE")) {
                validateReferenceMultiplicity(content, action, vr);
            } else if (action.startsWith("ADD CLASS")) {
                validateAddClass(content, action, vr);
            }
        }
    }

    private void validatePackageNsUri(String content,
                                      String action,
                                      ValidationResult vr) {

        Pattern p = Pattern.compile("nsURI:\\s*([^\\s]+)\\s*->\\s*([^\\s]+)");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String expectedNewUri = m.group(2).trim();

            if (!content.contains("nsURI=\"" + expectedNewUri + "\"")) {
                vr.addError("Expected nsURI not found: " + expectedNewUri);
            }
        }
    }

    private void validateRenameAttribute(String content,
                                         String action,
                                         ValidationResult vr) {

        Pattern p = Pattern.compile("rename:\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)\\s*->\\s*([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String oldClass = m.group(1);
            String oldAttr = m.group(2);
            String newClass = m.group(3);
            String newAttr = m.group(4);

            String classBlock = extractEClassBlock(content, newClass);
            if (classBlock == null) {
                vr.addError("Class not found for renamed attribute: " + newClass);
                return;
            }

            if (classBlock.contains("name=\"" + oldAttr + "\"")) {
                vr.addError("Old attribute still present after rename: " + oldClass + "::" + oldAttr);
            }

            if (!classBlock.contains("name=\"" + newAttr + "\"")) {
                vr.addError("New renamed attribute not found: " + newClass + "::" + newAttr);
            }
        }
    }

    private void validateDeleteAttribute(String content,
                                         String action,
                                         ValidationResult vr) {

        Pattern p = Pattern.compile("DELETE ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String className = m.group(1);
            String attrName = m.group(2);

            String classBlock = extractEClassBlock(content, className);
            if (classBlock != null && classBlock.contains("name=\"" + attrName + "\"")) {
                vr.addError("Deleted attribute still present: " + className + "::" + attrName);
            }
        }
    }

    private void validateAddAttribute(String content,
                                      String action,
                                      ValidationResult vr) {

        Pattern p = Pattern.compile("ADD ATTRIBUTE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String className = m.group(1);
            String attrName = m.group(2);

            String classBlock = extractEClassBlock(content, className);
            if (classBlock == null) {
                vr.addError("Class not found for added attribute: " + className);
                return;
            }

            if (!classBlock.contains("name=\"" + attrName + "\"")) {
                vr.addError("Added attribute not found: " + className + "::" + attrName);
            }
        }
    }

    private void validateReferenceMultiplicity(String content,
                                               String action,
                                               ValidationResult vr) {

        Pattern p = Pattern.compile("CHANGE REFERENCE\\s+([A-Za-z0-9_]+)::([A-Za-z0-9_]+).*\\[(\\-?\\d+),(\\-?\\d+)\\]\\s*->\\s*\\[(\\-?\\d+),(\\-?\\d+)\\]");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String className = m.group(1);
            String refName = m.group(2);
            String expectedUpper = m.group(6);

            String classBlock = extractEClassBlock(content, className);
            if (classBlock == null) {
                vr.addError("Class not found for changed reference: " + className);
                return;
            }

            Pattern refPattern = Pattern.compile(
                    "<eStructuralFeatures\\s+xsi:type=\"ecore:EReference\"([\\s\\S]*?)name=\"" + Pattern.quote(refName) + "\"([\\s\\S]*?)/>"
            );
            Matcher refMatcher = refPattern.matcher(classBlock);

            if (refMatcher.find()) {
                String refBlock = refMatcher.group(0);
                if (!refBlock.contains("upperBound=\"" + expectedUpper + "\"")) {
                    vr.addError("Reference upperBound not updated for: " + className + "::" + refName);
                }
            } else {
                vr.addError("Reference not found: " + className + "::" + refName);
            }
        }
    }

    private void validateAddClass(String content,
                                  String action,
                                  ValidationResult vr) {

        Pattern p = Pattern.compile("ADD CLASS\\s+([A-Za-z0-9_]+)");
        Matcher m = p.matcher(action);

        if (m.find()) {
            String className = m.group(1);

            if (!content.contains("name=\"" + className + "\"")) {
                vr.addError("Added class not found: " + className);
            }
        }
    }

    private String extractEClassBlock(String content, String className) {
        Pattern p = Pattern.compile(
                "<eClassifiers\\s+xsi:type=\"ecore:EClass\"\\s+name=\"" + Pattern.quote(className) + "\"[\\s\\S]*?</eClassifiers>"
        );
        Matcher m = p.matcher(content);

        if (m.find()) {
            return m.group(0);
        }

        return null;
    }

    public static class ValidationResult {
        private boolean valid;
        private MigrationResult migrationResult;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean v) { this.valid = v; }

        public MigrationResult getMigrationResult() { return migrationResult; }
        public void setMigrationResult(MigrationResult v) { this.migrationResult = v; }

        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public void addError(String e) { this.errors.add(e); }
        public void addWarning(String w) { this.warnings.add(w); }

        @Override
        public String toString() {
            return String.format(
                    "ValidationResult{valid=%b, errors=%d, warnings=%d}",
                    valid,
                    errors.size(),
                    warnings.size()
            );
        }
    }
}