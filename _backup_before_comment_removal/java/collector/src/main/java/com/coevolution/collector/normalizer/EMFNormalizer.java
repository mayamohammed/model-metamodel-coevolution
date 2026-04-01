package com.coevolution.collector.normalizer;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EMFNormalizer {

    public EMFNormalizer() {}

    
    public void normalize(File input, File output) throws Exception {
        if (input == null || !input.exists()) {
            throw new IllegalArgumentException("[EMFNormalizer] Fichier input introuvable : " + input);
        }
        if (output == null) {
            throw new IllegalArgumentException("[EMFNormalizer] Fichier output ne doit pas etre null");
        }

        
        File parentDir = output.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        
        ResourceSet resourceSet = createResourceSet();

        
        URI inputURI = URI.createFileURI(input.getAbsolutePath());
        Resource resource = resourceSet.getResource(inputURI, true);

        if (resource == null) {
            throw new RuntimeException("[EMFNormalizer] Impossible de charger la ressource : " + input);
        }

        System.out.println("[EMFNormalizer] Charge : " + input.getAbsolutePath()
                + " (" + resource.getContents().size() + " element(s) racine)");

        
        URI outputURI = URI.createFileURI(output.getAbsolutePath());
        resource.setURI(outputURI);
        resource.save(getSaveOptions());

        System.out.println("[EMFNormalizer] Normalise -> " + output.getAbsolutePath());
    }

    
    public String normalizeToString(File input) throws Exception {
        if (input == null || !input.exists()) {
            throw new IllegalArgumentException("[EMFNormalizer] Fichier input introuvable : " + input);
        }

        ResourceSet resourceSet = createResourceSet();
        URI inputURI = URI.createFileURI(input.getAbsolutePath());
        Resource resource = resourceSet.getResource(inputURI, true);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        resource.save(baos, getSaveOptions());
        return baos.toString("UTF-8");
    }

    
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();

        
        resourceSet.getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());

        
        resourceSet.getPackageRegistry()
                .put(org.eclipse.emf.ecore.EcorePackage.eNS_URI,
                     org.eclipse.emf.ecore.EcorePackage.eINSTANCE);

        return resourceSet;
    }

    
    private Map<String, Object> getSaveOptions() {
        Map<String, Object> options = new HashMap<>();

        
        options.put(XMLResource.OPTION_ENCODING, "UTF-8");

        
        options.put(XMLResource.OPTION_FORMATTED, Boolean.TRUE);

        
        options.put(XMLResource.OPTION_LINE_WIDTH, 80);

        
        options.put(XMLResource.OPTION_SAVE_DOCTYPE, Boolean.FALSE);

        
        options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);

        return options;
    }

    
    public boolean isValidEcore(File file) {
        if (file == null || !file.exists() || !file.getName().endsWith(".ecore")) {
            return false;
        }
        try {
            ResourceSet resourceSet = createResourceSet();
            URI uri = URI.createFileURI(file.getAbsolutePath());
            Resource resource = resourceSet.getResource(uri, true);
            return resource != null && !resource.getContents().isEmpty();
        } catch (Exception e) {
            System.out.println("[EMFNormalizer] Fichier invalide : " + file.getName() + " -> " + e.getMessage());
            return false;
        }
    }
}
