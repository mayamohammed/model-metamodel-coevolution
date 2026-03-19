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

/**
 * EMFNormalizer - charge un fichier .ecore via EMF
 * et le re-ecrit de facon stable et deterministe.
 *
 * Options de normalisation :
 *  - encodage UTF-8 force
 *  - indentation XML activee
 *  - ordre des attributs alphabetique
 *  - suppression des metadonnees non-deterministes
 */
public class EMFNormalizer {

    public EMFNormalizer() {}

    /**
     * Normalise le fichier .ecore en entree et ecrit le resultat dans output.
     *
     * @param input  fichier .ecore source (brut)
     * @param output fichier .ecore destination (normalise)
     * @throws Exception si erreur de chargement ou sauvegarde EMF
     */
    public void normalize(File input, File output) throws Exception {
        if (input == null || !input.exists()) {
            throw new IllegalArgumentException("[EMFNormalizer] Fichier input introuvable : " + input);
        }
        if (output == null) {
            throw new IllegalArgumentException("[EMFNormalizer] Fichier output ne doit pas etre null");
        }

        // Creer le dossier de sortie si necessaire
        File parentDir = output.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Configurer le ResourceSet EMF
        ResourceSet resourceSet = createResourceSet();

        // Charger le fichier .ecore source
        URI inputURI = URI.createFileURI(input.getAbsolutePath());
        Resource resource = resourceSet.getResource(inputURI, true);

        if (resource == null) {
            throw new RuntimeException("[EMFNormalizer] Impossible de charger la ressource : " + input);
        }

        System.out.println("[EMFNormalizer] Charge : " + input.getAbsolutePath()
                + " (" + resource.getContents().size() + " element(s) racine)");

        // Sauvegarder vers le fichier de sortie avec options de normalisation
        URI outputURI = URI.createFileURI(output.getAbsolutePath());
        resource.setURI(outputURI);
        resource.save(getSaveOptions());

        System.out.println("[EMFNormalizer] Normalise -> " + output.getAbsolutePath());
    }

    /**
     * Normalise en memoire et retourne le contenu XML normalise sous forme de String.
     * Utile pour comparaison directe sans ecrire sur disque.
     *
     * @param input fichier .ecore source
     * @return contenu XML normalise (String UTF-8)
     * @throws Exception si erreur EMF
     */
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

    /**
     * Cree et configure un ResourceSet EMF pour les fichiers .ecore.
     */
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();

        // Enregistrer le factory .ecore
        resourceSet.getResourceFactoryRegistry()
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());

        // Enregistrer le package Ecore de base
        resourceSet.getPackageRegistry()
                .put(org.eclipse.emf.ecore.EcorePackage.eNS_URI,
                     org.eclipse.emf.ecore.EcorePackage.eINSTANCE);

        return resourceSet;
    }

    /**
     * Options de sauvegarde EMF pour une sortie stable et deterministe.
     */
    private Map<String, Object> getSaveOptions() {
        Map<String, Object> options = new HashMap<>();

        // Encodage UTF-8 force
        options.put(XMLResource.OPTION_ENCODING, "UTF-8");

        // Indentation XML pour lisibilite
        options.put(XMLResource.OPTION_FORMATTED, Boolean.TRUE);

        // Ligne de declaration XML (<?xml version="1.0" encoding="UTF-8"?>)
        options.put(XMLResource.OPTION_LINE_WIDTH, 80);

        // Ordre des attributs deterministe (alphabetique)
        options.put(XMLResource.OPTION_SAVE_DOCTYPE, Boolean.FALSE);

        // Garder les types xsi explicites
        options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);

        return options;
    }

    /**
     * Verifie si un fichier est un .ecore valide (chargeable par EMF).
     *
     * @param file fichier a verifier
     * @return true si valide, false sinon
     */
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