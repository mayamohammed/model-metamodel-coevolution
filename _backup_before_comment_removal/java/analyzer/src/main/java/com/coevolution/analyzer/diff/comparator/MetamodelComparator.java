package com.coevolution.analyzer.diff.comparator;

import org.eclipse.emf.ecore.*;
import java.util.*;

public class MetamodelComparator {
    public static class Snapshot {
        public final EPackage pkg;
        public final Map<String, EClass> classes = new LinkedHashMap<>();
        public final Map<String, EStructuralFeature> features = new LinkedHashMap<>();
        public Snapshot(EPackage pkg) {
            this.pkg = pkg;
            for (EClassifier c : pkg.getEClassifiers()) {
                if (c instanceof EClass) {
                    EClass ec = (EClass) c;
                    classes.put(ec.getName(), ec);
                    for (EStructuralFeature f : ec.getEStructuralFeatures())
                        features.put(ec.getName() + "::" + f.getName(), f);
                }
            }
        }
    }
    public Snapshot snapshot(EPackage pkg) { return new Snapshot(pkg); }
}