package com.coevolution.analyzer.diff.diff;

import com.coevolution.analyzer.diff.comparator.MetamodelComparator;
import com.coevolution.analyzer.diff.model.DiffDelta;
import org.eclipse.emf.ecore.*;
import java.util.*;

public class DiffCategorizer {
    public List<DiffDelta> categorize(MetamodelComparator.Snapshot s1, MetamodelComparator.Snapshot s2) {
        List<DiffDelta> deltas = new ArrayList<>();
        // Classes added/removed
        for (String name : s2.classes.keySet())
            if (!s1.classes.containsKey(name))
                deltas.add(new DiffDelta(DiffDelta.Kind.ADD, DiffDelta.ElementType.CLASS, name, "EClass added"));
        for (String name : s1.classes.keySet())
            if (!s2.classes.containsKey(name))
                deltas.add(new DiffDelta(DiffDelta.Kind.DELETE, DiffDelta.ElementType.CLASS, name, "EClass removed"));
        // Features added/removed
        for (String key : s2.features.keySet()) {
            if (!s1.features.containsKey(key)) {
                EStructuralFeature f = s2.features.get(key);
                DiffDelta.ElementType t = f instanceof EReference ? DiffDelta.ElementType.REFERENCE : DiffDelta.ElementType.ATTRIBUTE;
                deltas.add(new DiffDelta(DiffDelta.Kind.ADD, t, key, "Feature added"));
            }
        }
        for (String key : s1.features.keySet()) {
            if (!s2.features.containsKey(key)) {
                EStructuralFeature f = s1.features.get(key);
                DiffDelta.ElementType t = f instanceof EReference ? DiffDelta.ElementType.REFERENCE : DiffDelta.ElementType.ATTRIBUTE;
                deltas.add(new DiffDelta(DiffDelta.Kind.DELETE, t, key, "Feature removed"));
            }
        }
        return deltas;
    }
}