package com.coevolution.core.emf;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Configures the EMF ResourceSet for loading .ecore and .xmi files.
 */
public class ResourceSetConfig {

    /**
     * Creates a ResourceSet configured for .ecore files.
     */
    public static ResourceSet createForEcore() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("ecore", new EcoreResourceFactoryImpl());
        rs.getPackageRegistry()
          .put(EPackage.Registry.INSTANCE.toString(),
               EPackage.Registry.INSTANCE);
        return rs;
    }

    /**
     * Creates a ResourceSet configured for .xmi files.
     */
    public static ResourceSet createForXmi() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("xmi", new XMIResourceFactoryImpl());
        return rs;
    }

    /**
     * Creates a ResourceSet configured for both .ecore and .xmi.
     */
    public static ResourceSet createForBoth() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("xmi", new XMIResourceFactoryImpl());
        return rs;
    }
}