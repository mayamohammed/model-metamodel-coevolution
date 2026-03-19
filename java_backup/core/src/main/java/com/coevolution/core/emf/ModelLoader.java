package com.coevolution.core.emf;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads XMI model instances (.xmi files) using EMF.
 */
public class ModelLoader {

    private final ResourceSet resourceSet;

    public ModelLoader() {
        this.resourceSet = ResourceSetConfig.createForXmi();
    }

    /**
     * Loads a single .xmi file.
     */
    public Resource load(String xmiPath, EPackage ePackage) {
        File file = new File(xmiPath);

        if (!file.exists()) {
            throw new RuntimeException(
                "Model file not found : " + xmiPath
            );
        }
        if (!xmiPath.endsWith(".xmi")) {
            throw new RuntimeException(
                "File is not .xmi : " + xmiPath
            );
        }

        try {
            resourceSet.getPackageRegistry()
                       .put(ePackage.getNsURI(), ePackage);

            URI uri = URI.createFileURI(
                file.getAbsolutePath()
            );
            Resource resource =
                resourceSet.getResource(uri, true);
            resource.load(null);

            System.out.println(
                "[ModelLoader] Loaded : " + xmiPath
                + " (" + resource.getContents().size()
                + " objects)"
            );
            return resource;

        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to load : " + xmiPath
                + " → " + e.getMessage(), e
            );
        }
    }

    /**
     * Loads all .xmi files from a directory.
     */
    public List<Resource> loadAll(String dirPath,
                                   EPackage ePackage) {
        List<Resource> resources = new ArrayList<>();
        File dir = new File(dirPath);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException(
                "Directory not found : " + dirPath
            );
        }

        File[] files = dir.listFiles(
            (d, name) -> name.endsWith(".xmi")
        );

        if (files == null || files.length == 0) {
            System.out.println(
                "[ModelLoader] No .xmi in : " + dirPath
            );
            return resources;
        }

        for (File f : files) {
            try {
                Resource r = load(
                    f.getAbsolutePath(), ePackage
                );
                resources.add(r);
            } catch (Exception e) {
                System.err.println(
                    "[ModelLoader] ERROR : "
                    + f.getName()
                    + " → " + e.getMessage()
                );
            }
        }

        System.out.println(
            "[ModelLoader] Loaded "
            + resources.size() + " models"
        );
        return resources;
    }

    /**
     * Returns all EObjects from a Resource.
     */
    public List<EObject> getObjects(Resource resource) {
        List<EObject> objects = new ArrayList<>();
        resource.getAllContents()
                .forEachRemaining(objects::add);
        return objects;
    }
}