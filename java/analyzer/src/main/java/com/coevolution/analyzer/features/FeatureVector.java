
package com.coevolution.analyzer.features;

public class FeatureVector {

    public static final String[] FEATURE_NAMES = {
        "nb_classes_v1",
        "nb_classes_v2",
        "delta_classes",
        "nb_added_classes",
        "nb_removed_classes",
        "nb_attributes_v1",
        "nb_attributes_v2",
        "delta_attributes",
        "nb_added_attributes",
        "nb_removed_attributes",
        "nb_type_changes",
        "nb_references_v1",
        "nb_references_v2",
        "delta_references",
        "nb_added_references",
        "nb_removed_references",
        "nb_multiplicity_changes",
        "nb_containment_changes",
        "nb_abstract_changes",
        "nb_supertype_changes",
        "nsuri_changed"
    };

    private final String pairId;
    private final double[] values;
    private String label;   

    public FeatureVector(String pairId, double[] values, String label) {
        this.pairId = pairId;
        this.values = values;
        this.label  = label;
    }

    public String   getPairId() { return pairId; }
    public double[] getValues() { return values; }
    public String   getLabel()  { return label;  }
    public void     setLabel(String l) { this.label = l; }

    public static int size() { return FEATURE_NAMES.length; }

    
    public String toCsvRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(pairId);
        for (double v : values) sb.append(",").append((int) v);
        sb.append(",").append(label != null ? label : "UNKNOWN");
        return sb.toString();
    }

    
    public static String csvHeader() {
        StringBuilder sb = new StringBuilder("pair_id");
        for (String f : FEATURE_NAMES) sb.append(",").append(f);
        sb.append(",label");
        return sb.toString();
    }
}
