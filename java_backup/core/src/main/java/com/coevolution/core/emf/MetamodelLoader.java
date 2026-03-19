package com.coevolution.core.emf;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads Ecore metamodels (.ecore files) using EMF.
 */
public class MetamodelLoader {

    private final ResourceSet resourceSet;

    public MetamodelLoader() {
        this.resourceSet = ResourceSetConfig.createForEcore();
    }

    /**
     * Loads a single .ecore file.
     */
    public EPackage load(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException(
                "Metamodel file not found : " + filePath
            );
        }
        if (!filePath.endsWith(".ecore")) {
            throw new RuntimeException(
                "File is not .ecore : " + filePath
            );
        }

        try {
            URI uri = URI.createFileURI(file.getAbsolutePath());
            Resource resource = resourceSet.getResource(uri, true);
            resource.load(null);

            if (resource.getContents().isEmpty()) {
                throw new RuntimeException(
                    "Metamodel is empty : " + filePath
                );
            }

            EPackage ePackage =
                (EPackage) resource.getContents().get(0);

            System.out.println(
                "[MetamodelLoader] Loaded : "
                + ePackage.getName()
                + " from " + filePath
            );
            return ePackage;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load : " + filePath
                + " → " + e.getMessage(), e
            );
        }
    }

    /**
     * Loads all .ecore files from a directory.
     */
    public List<EPackage> loadAll(String directoryPath) {
        List<EPackage> packages = new ArrayList<>();
        File dir = new File(directoryPath);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException(
                "Directory not found : " + directoryPath
            );
        }

        File[] files = dir.listFiles(
            (d, name) -> name.endsWith(".ecore")
        );

        if (files == null || files.length == 0) {
            System.out.println(
                "[MetamodelLoader] No .ecore in : "
                + directoryPath
            );
            return packages;
        }

        for (File f : files) {
            EPackage pkg = load(f.getAbsolutePath());
            packages.add(pkg);
        }

        System.out.println(
            "[MetamodelLoader] Loaded "
            + packages.size() + " metamodels"
        );
        return packages;
    }

    /**
     * Returns true if file is a valid .ecore.
     */
    public boolean isValid(String filePath) {
        try {
            load(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}