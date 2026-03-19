package com.coevolution.core.model;

/**
 * Configuration for a domain.
 */
public class DomainConfig {

    private final String name;
    private final String metamodelsPath;
    private final String modelsV1Path;
    private final String modelsV2Path;
    private final String modelsV3Path;

    public DomainConfig(String name,
                         String baseDataPath) {
        this.name           = name;
        this.metamodelsPath = baseDataPath
            + "/domains/" + name + "/metamodels";
        this.modelsV1Path   = baseDataPath
            + "/domains/" + name + "/models/v1";
        this.modelsV2Path   = baseDataPath
            + "/domains/" + name + "/models/v2";
        this.modelsV3Path   = baseDataPath
            + "/domains/" + name + "/models/v3";
    }

    public String getName()           { return name;           }
    public String getMetamodelsPath() { return metamodelsPath; }
    public String getModelsV1Path()   { return modelsV1Path;   }
    public String getModelsV2Path()   { return modelsV2Path;   }
    public String getModelsV3Path()   { return modelsV3Path;   }

    public String getMetamodelPath(String version) {
        return metamodelsPath + "/"
            + name + "_" + version + ".ecore";
    }

    public String getModelsPath(String version) {
        switch (version) {
            case "v1": return modelsV1Path;
            case "v2": return modelsV2Path;
            case "v3": return modelsV3Path;
            default: throw new RuntimeException(
                "Unknown version : " + version
            );
        }
    }

    @Override
    public String toString() {
        return "DomainConfig{name=" + name + "}";
    }
}