package com.coevolution.dataset.error;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MigrationErrorDetector {

    public enum ErrorType {
        BREAKING_REMOVAL,
        TYPE_INCOMPATIBILITY,
        MULTIPLICITY_VIOLATION,
        CONTAINMENT_CONFLICT,
        MISSING_SUPERTYPE,
        NAMESPACE_CHANGE
    }

    public static class MigrationError {
        private final ErrorType type;
        private final String    element;
        private final String    description;
        private final String    severity;

        public MigrationError(ErrorType type, String element,
                              String description, String severity) {
            this.type        = type;
            this.element     = element;
            this.description = description;
            this.severity    = severity;
        }

        public ErrorType getType()        { return type;        }
        public String    getElement()     { return element;     }
        public String    getDescription() { return description; }
        public String    getSeverity()    { return severity;    }

        @Override
        public String toString() {
            return String.format("[%s] %s : %s (%s)",
                severity, type, element, description);
        }
    }

    public List<MigrationError> detect(Map<String, Integer> features) {
        List<MigrationError> errors = new ArrayList<>();

        int removedClasses    = features.getOrDefault("nb_removed_classes", 0);
        int removedAttributes = features.getOrDefault("nb_removed_attributes", 0);
        int typeChanges       = features.getOrDefault("nb_type_changes", 0);
        int multiplicityChg   = features.getOrDefault("nb_multiplicity_changes", 0);
        int containmentChg    = features.getOrDefault("nb_containment_changes", 0);
        int supertypeChg      = features.getOrDefault("nb_supertype_changes", 0);
        int nsUriChanged      = features.getOrDefault("nsuri_changed", 0);

        if (removedClasses > 0)
            errors.add(new MigrationError(ErrorType.BREAKING_REMOVAL,
                "EClass",
                removedClasses + " classe(s) supprimee(s) - migration impossible",
                "HIGH"));

        if (removedAttributes > 0)
            errors.add(new MigrationError(ErrorType.BREAKING_REMOVAL,
                "EAttribute",
                removedAttributes + " attribut(s) supprime(s) - donnees perdues",
                "HIGH"));

        if (typeChanges > 0)
            errors.add(new MigrationError(ErrorType.TYPE_INCOMPATIBILITY,
                "EAttribute.eType",
                typeChanges + " type(s) change(s) - conversion requise",
                "HIGH"));

        if (multiplicityChg > 0)
            errors.add(new MigrationError(ErrorType.MULTIPLICITY_VIOLATION,
                "EReference.multiplicity",
                multiplicityChg + " multiplicite(s) modifiee(s)",
                "MEDIUM"));

        if (containmentChg > 0)
            errors.add(new MigrationError(ErrorType.CONTAINMENT_CONFLICT,
                "EReference.containment",
                containmentChg + " containment(s) modifie(s)",
                "MEDIUM"));

        if (supertypeChg > 0)
            errors.add(new MigrationError(ErrorType.MISSING_SUPERTYPE,
                "EClass.eSuperTypes",
                supertypeChg + " supertype(s) modifie(s)",
                "MEDIUM"));

        if (nsUriChanged > 0)
            errors.add(new MigrationError(ErrorType.NAMESPACE_CHANGE,
                "EPackage.nsURI",
                "NsURI modifie - references cassees",
                "LOW"));

        return errors;
    }

    public Map<String, Long> summarize(List<MigrationError> errors) {
        Map<String, Long> summary = new HashMap<>();
        summary.put("HIGH",   errors.stream().filter(e -> "HIGH".equals(e.getSeverity())).count());
        summary.put("MEDIUM", errors.stream().filter(e -> "MEDIUM".equals(e.getSeverity())).count());
        summary.put("LOW",    errors.stream().filter(e -> "LOW".equals(e.getSeverity())).count());
        summary.put("TOTAL",  (long) errors.size());
        return summary;
    }

    public boolean hasBreakingErrors(List<MigrationError> errors) {
        return errors.stream().anyMatch(e -> "HIGH".equals(e.getSeverity()));
    }
}