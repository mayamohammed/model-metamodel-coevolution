package com.coevolution.dataset.label;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelMapper {

    
    public enum PrimaryLabel {
        CLASS_CHANGE,
        ATTRIBUTE_CHANGE,
        REFERENCE_CHANGE,
        TYPE_CHANGE,
        STRUCTURAL_CHANGE,
        MIXED
    }

    
    public static final String ECLASS_ADDED                    = "ECLASS_ADDED";
    public static final String ECLASS_REMOVED                  = "ECLASS_REMOVED";
    public static final String ECLASS_ABSTRACT_CHANGED         = "ECLASS_ABSTRACT_CHANGED";
    public static final String ECLASS_SUPERTYPE_ADDED          = "ECLASS_SUPERTYPE_ADDED";
    public static final String EATTRIBUTE_ADDED                = "EATTRIBUTE_ADDED";
    public static final String EATTRIBUTE_REMOVED              = "EATTRIBUTE_REMOVED";
    public static final String EATTRIBUTE_TYPE_CHANGED         = "EATTRIBUTE_TYPE_CHANGED";
    public static final String EREFERENCE_ADDED                = "EREFERENCE_ADDED";
    public static final String EREFERENCE_REMOVED              = "EREFERENCE_REMOVED";
    public static final String EREFERENCE_MULTIPLICITY_CHANGED = "EREFERENCE_MULTIPLICITY_CHANGED";
    public static final String EREFERENCE_CONTAINMENT_CHANGED  = "EREFERENCE_CONTAINMENT_CHANGED";
    public static final String MIXED                           = "MIXED";

    public static final List<String> ALL_LABELS = Arrays.asList(
        ECLASS_ADDED, ECLASS_REMOVED, ECLASS_ABSTRACT_CHANGED,
        ECLASS_SUPERTYPE_ADDED, EATTRIBUTE_ADDED, EATTRIBUTE_REMOVED,
        EATTRIBUTE_TYPE_CHANGED, EREFERENCE_ADDED, EREFERENCE_REMOVED,
        EREFERENCE_MULTIPLICITY_CHANGED, EREFERENCE_CONTAINMENT_CHANGED,
        MIXED
    );

    private static final Map<String, PrimaryLabel> DETAIL_TO_PRIMARY = new HashMap<>();
    static {
        DETAIL_TO_PRIMARY.put(ECLASS_ADDED,                    PrimaryLabel.CLASS_CHANGE);
        DETAIL_TO_PRIMARY.put(ECLASS_REMOVED,                  PrimaryLabel.CLASS_CHANGE);
        DETAIL_TO_PRIMARY.put(ECLASS_ABSTRACT_CHANGED,         PrimaryLabel.STRUCTURAL_CHANGE);
        DETAIL_TO_PRIMARY.put(ECLASS_SUPERTYPE_ADDED,          PrimaryLabel.STRUCTURAL_CHANGE);
        DETAIL_TO_PRIMARY.put(EATTRIBUTE_ADDED,                PrimaryLabel.ATTRIBUTE_CHANGE);
        DETAIL_TO_PRIMARY.put(EATTRIBUTE_REMOVED,              PrimaryLabel.ATTRIBUTE_CHANGE);
        DETAIL_TO_PRIMARY.put(EATTRIBUTE_TYPE_CHANGED,         PrimaryLabel.TYPE_CHANGE);
        DETAIL_TO_PRIMARY.put(EREFERENCE_ADDED,                PrimaryLabel.REFERENCE_CHANGE);
        DETAIL_TO_PRIMARY.put(EREFERENCE_REMOVED,              PrimaryLabel.REFERENCE_CHANGE);
        DETAIL_TO_PRIMARY.put(EREFERENCE_MULTIPLICITY_CHANGED, PrimaryLabel.REFERENCE_CHANGE);
        DETAIL_TO_PRIMARY.put(EREFERENCE_CONTAINMENT_CHANGED,  PrimaryLabel.REFERENCE_CHANGE);
        DETAIL_TO_PRIMARY.put(MIXED,                           PrimaryLabel.MIXED);
    }

    public String mapToDetailedLabel(Map<String, Integer> features) {
        int addedClasses      = features.getOrDefault("nb_added_classes",      0);
        int removedClasses    = features.getOrDefault("nb_removed_classes",    0);
        int addedAttributes   = features.getOrDefault("nb_added_attributes",   0);
        int removedAttributes = features.getOrDefault("nb_removed_attributes", 0);
        int typeChanges       = features.getOrDefault("nb_type_changes",       0);
        int addedReferences   = features.getOrDefault("nb_added_references",   0);
        int removedReferences = features.getOrDefault("nb_removed_references", 0);
        int multiplicityChg   = features.getOrDefault("nb_multiplicity_changes", 0);
        int containmentChg    = features.getOrDefault("nb_containment_changes",  0);
        int abstractChg       = features.getOrDefault("nb_abstract_changes",     0);
        int supertypeChg      = features.getOrDefault("nb_supertype_changes",    0);

        int activeTypes = 0;
        if (addedClasses      > 0) activeTypes++;
        if (removedClasses    > 0) activeTypes++;
        if (addedAttributes   > 0) activeTypes++;
        if (removedAttributes > 0) activeTypes++;
        if (typeChanges       > 0) activeTypes++;
        if (addedReferences   > 0) activeTypes++;
        if (removedReferences > 0) activeTypes++;
        if (multiplicityChg   > 0) activeTypes++;
        if (containmentChg    > 0) activeTypes++;
        if (abstractChg       > 0) activeTypes++;
        if (supertypeChg      > 0) activeTypes++;

        if (activeTypes > 1)      return MIXED;
        if (addedClasses      > 0) return ECLASS_ADDED;
        if (removedClasses    > 0) return ECLASS_REMOVED;
        if (abstractChg       > 0) return ECLASS_ABSTRACT_CHANGED;
        if (supertypeChg      > 0) return ECLASS_SUPERTYPE_ADDED;
        if (addedAttributes   > 0) return EATTRIBUTE_ADDED;
        if (removedAttributes > 0) return EATTRIBUTE_REMOVED;
        if (typeChanges       > 0) return EATTRIBUTE_TYPE_CHANGED;
        if (addedReferences   > 0) return EREFERENCE_ADDED;
        if (removedReferences > 0) return EREFERENCE_REMOVED;
        if (multiplicityChg   > 0) return EREFERENCE_MULTIPLICITY_CHANGED;
        if (containmentChg    > 0) return EREFERENCE_CONTAINMENT_CHANGED;
        return MIXED;
    }

    public PrimaryLabel mapToPrimaryLabel(String detailedLabel) {
        return DETAIL_TO_PRIMARY.getOrDefault(detailedLabel, PrimaryLabel.MIXED);
    }

    public List<String> getAllDetailedLabels()   { return ALL_LABELS;              }
    public PrimaryLabel[] getAllPrimaryLabels()  { return PrimaryLabel.values();   }
    public boolean isValidLabel(String label)   { return ALL_LABELS.contains(label); }

    public Map<String, Integer> computeStats(List<String> labels) {
        Map<String, Integer> stats = new HashMap<>();
        for (String l : ALL_LABELS) stats.put(l, 0);
        for (String l : labels) stats.merge(l, 1, Integer::sum);
        return stats;
    }
}