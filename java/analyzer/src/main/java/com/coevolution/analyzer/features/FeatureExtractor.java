
package com.coevolution.analyzer.features;

import com.coevolution.analyzer.diff.EcoreDiff;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.emf.ecore.EPackage;
import java.io.File;

public class FeatureExtractor {

    private final ObjectMapper mapper = new ObjectMapper();

    
    public FeatureVector extract(File pairDir) throws Exception {
        File v1File       = new File(pairDir, "v1.ecore");
        File v2File       = new File(pairDir, "v2.ecore");
        File manifestFile = new File(pairDir, "manifest.json");

        if (!v1File.exists() || !v2File.exists()) {
            throw new IllegalArgumentException(
                    "v1.ecore ou v2.ecore manquant dans : "
                    + pairDir.getName());
        }

        
        EPackage v1 = EcoreDiff.load(v1File);
        EPackage v2 = EcoreDiff.load(v2File);

        
        double[] values = new double[FeatureVector.size()];

        int nbClassV1  = EcoreDiff.getClasses(v1).size();
        int nbClassV2  = EcoreDiff.getClasses(v2).size();
        int nbAttrV1   = EcoreDiff.countAttributes(v1);
        int nbAttrV2   = EcoreDiff.countAttributes(v2);
        int nbRefV1    = EcoreDiff.countReferences(v1);
        int nbRefV2    = EcoreDiff.countReferences(v2);

        values[0]  = nbClassV1;
        values[1]  = nbClassV2;
        values[2]  = nbClassV2 - nbClassV1;
        values[3]  = EcoreDiff.getAddedClasses(v1, v2).size();
        values[4]  = EcoreDiff.getRemovedClasses(v1, v2).size();
        values[5]  = nbAttrV1;
        values[6]  = nbAttrV2;
        values[7]  = nbAttrV2 - nbAttrV1;
        values[8]  = EcoreDiff.countAddedAttributes(v1, v2);
        values[9]  = EcoreDiff.countRemovedAttributes(v1, v2);
        values[10] = EcoreDiff.countTypeChanges(v1, v2);
        values[11] = nbRefV1;
        values[12] = nbRefV2;
        values[13] = nbRefV2 - nbRefV1;
        values[14] = EcoreDiff.countAddedReferences(v1, v2);
        values[15] = EcoreDiff.countRemovedReferences(v1, v2);
        values[16] = EcoreDiff.countMultiplicityChanges(v1, v2);
        values[17] = EcoreDiff.countContainmentChanges(v1, v2);
        values[18] = EcoreDiff.countAbstractChanges(v1, v2);
        values[19] = EcoreDiff.countSuperTypeChanges(v1, v2);
        values[20] = EcoreDiff.nsUriChanged(v1, v2);

        
        String label = "UNKNOWN";
        if (manifestFile.exists()) {
            JsonNode root = mapper.readTree(manifestFile);
            
            if (root.has("ground_truth")) {
                label = root.get("ground_truth")
                            .get("change_type").asText("UNKNOWN");
            }
            
            else if (root.has("change_type")) {
                label = root.get("change_type").asText("UNKNOWN");
            }
            
            else {
                label = "MIXED";
            }
        }

        return new FeatureVector(pairDir.getName(), values, label);
    }
}
