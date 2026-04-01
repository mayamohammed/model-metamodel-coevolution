package com.coevolution.core.emf;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class ResourceSetConfig {

    
   
public static ResourceSet createForEcore() {
    ResourceSet rs = new ResourceSetImpl();
    rs.getResourceFactoryRegistry()
      .getExtensionToFactoryMap()
      .put("ecore", new EcoreResourceFactoryImpl());
    
    return rs;
}

    
    public static ResourceSet createForXmi() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("xmi", new XMIResourceFactoryImpl());
        return rs;
    }

    
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
