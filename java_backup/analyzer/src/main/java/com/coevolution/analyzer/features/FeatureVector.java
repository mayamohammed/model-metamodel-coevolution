
package com.coevolution.analyzer.features;

/**
 * FeatureVector — vecteur de 20 features numeriques
 * extrait d une paire (v1.ecore, v2.ecore).
 *
 * Ces features sont utilisees par le modele ML
 * pour predire le type de changement.
 *
 * FEATURES :
 *  [0]  nb_classes_v1
 *  [1]  nb_classes_v2
 *  [2]  delta_classes         = v2 - v1
 *  [3]  nb_added_classes
 *  [4]  nb_removed_classes
 *  [5]  nb_attributes_v1
 *  [6]  nb_attributes_v2
 *  [7]  delta_attributes      = v2 - v1
 *  [8]  nb_added_attributes
 *  [9]  nb_removed_attributes
 *  [10] nb_type_changes
 *  [11] nb_references_v1
 *  [12] nb_references_v2
 *  [13] delta_references      = v2 - v1
 *  [14] nb_added_references
 *  [15] nb_removed_references
 *  [16] nb_multiplicity_changes
 *  [17] nb_containment_changes
 *  [18] nb_abstract_changes
 *  [19] nb_supertype_changes
 *  [20] nsuri_changed         = 0 ou 1
 */
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
    private String label;   // ground truth : ECLASS_ADDED, etc.

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

    /** Retourne une ligne CSV : pairId, f1, f2, ..., label */
    public String toCsvRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(pairId);
        for (double v : values) sb.append(",").append((int) v);
        sb.append(",").append(label != null ? label : "UNKNOWN");
        return sb.toString();
    }

    /** En-tete CSV */
    public static String csvHeader() {
        StringBuilder sb = new StringBuilder("pair_id");
        for (String f : FEATURE_NAMES) sb.append(",").append(f);
        sb.append(",label");
        return sb.toString();
    }
}