package com.coevolution.core.model;

import org.eclipse.emf.ecore.EPackage;

/**
 * Represents a versioned metamodel (V1, V2 or V3).
 */
public class MetamodelVersion {

    private final String   domain;
    private final String   version;
    private final String   filePath;
    private final EPackage ePackage;

    public MetamodelVersion(String domain,
                             String version,
                             String filePath,
                             EPackage ePackage) {
        this.domain   = domain;
        this.version  = version;
        this.filePath = filePath;
        this.ePackage = ePackage;
    }

    public String   getDomain()   { return domain;   }
    public String   getVersion()  { return version;  }
    public String   getFilePath() { return filePath; }
    public EPackage getEPackage() { return ePackage; }

    public String getId() {
        return domain + "_" + version;
    }

    @Override
    public String toString() {
        return "MetamodelVersion{"
            + "id="      + getId()
            + ", file="  + filePath
            + ", nsURI=" + (ePackage != null
                ? ePackage.getNsURI() : "null")
            + "}";
    }
}