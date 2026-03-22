package com.coevolution.analyzer.label;

import java.util.HashMap;
import java.util.Map;

public class LabelMapper {

    private static final Map<String, String> KEYWORD_MAP = new HashMap<>();

    static {
        KEYWORD_MAP.put("addclass",        "ECLASS_ADDED");
        KEYWORD_MAP.put("removeclass",     "ECLASS_REMOVED");
        KEYWORD_MAP.put("addattribute",    "EATTRIBUTE_ADDED");
        KEYWORD_MAP.put("removeattribute", "EATTRIBUTE_REMOVED");
        KEYWORD_MAP.put("typechange",      "EATTRIBUTE_TYPE_CHANGED");
        KEYWORD_MAP.put("addreference",    "EREFERENCE_ADDED");
        KEYWORD_MAP.put("removereference", "EREFERENCE_REMOVED");
        KEYWORD_MAP.put("multiplicity",    "EREFERENCE_MULTIPLICITY_CHANGED");
        KEYWORD_MAP.put("abstract",        "ECLASS_ABSTRACT_CHANGED");
        KEYWORD_MAP.put("supertype",       "ECLASS_SUPERTYPE_ADDED");
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "MIXED";
        String cleaned = raw.toUpperCase()
                            .replace("-", "_")
                            .replace(" ", "_")
                            .trim();
        String[] valid = {
            "ECLASS_ADDED","ECLASS_REMOVED",
            "EATTRIBUTE_ADDED","EATTRIBUTE_REMOVED","EATTRIBUTE_TYPE_CHANGED",
            "EREFERENCE_ADDED","EREFERENCE_REMOVED","EREFERENCE_MULTIPLICITY_CHANGED",
            "ECLASS_ABSTRACT_CHANGED","ECLASS_SUPERTYPE_ADDED","MIXED"
        };
        for (String v : valid) {
            if (cleaned.equals(v)) return v;
        }
        return inferFromName(raw);
    }

    public static String inferFromName(String name) {
        if (name == null) return "MIXED";
        String lower = name.toLowerCase().replace("_","").replace("-","");
        for (Map.Entry<String, String> e : KEYWORD_MAP.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return "MIXED";
    }

    public static void main(String[] args) {
        String[] tests = {
            "eclass_added", "ECLASS-REMOVED", "addattribute",
            "supertype", "unknown_change", null
        };
        System.out.println("=== LabelMapper Test ===");
        for (String t : tests) {
            System.out.println("  " + t + " -> " + normalize(t));
        }
    }
}
