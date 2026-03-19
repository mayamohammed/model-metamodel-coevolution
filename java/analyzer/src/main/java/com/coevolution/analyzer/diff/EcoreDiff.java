package com.coevolution.analyzer.diff;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.EcorePackage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class EcoreDiff {

    public static EPackage load(File f) throws Exception {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("ecore", new XMIResourceFactoryImpl());
        rs.getPackageRegistry()
          .put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        Resource res = rs.getResource(
                URI.createFileURI(f.getAbsolutePath()), true);
        return (EPackage) res.getContents().get(0);
    }

    public static List<EClass> getClasses(EPackage pkg) {
        return pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c)
                .collect(Collectors.toList());
    }

    public static Set<String> getClassNames(EPackage pkg) {
        return getClasses(pkg).stream()
                .map(EClass::getName)
                .collect(Collectors.toSet());
    }

    public static Set<String> getAddedClasses(EPackage v1, EPackage v2) {
        Set<String> s = new HashSet<>(getClassNames(v2));
        s.removeAll(getClassNames(v1));
        return s;
    }

    public static Set<String> getRemovedClasses(EPackage v1, EPackage v2) {
        Set<String> s = new HashSet<>(getClassNames(v1));
        s.removeAll(getClassNames(v2));
        return s;
    }

    public static int countAttributes(EPackage pkg) {
        return getClasses(pkg).stream()
                .mapToInt(c -> c.getEAttributes().size()).sum();
    }

    public static int countAddedAttributes(EPackage v1, EPackage v2) {
        int added = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            Set<String> a1 = attrNames(c1);
            for (EAttribute a : c2.getEAttributes())
                if (!a1.contains(a.getName())) added++;
        }
        return added;
    }

    public static int countRemovedAttributes(EPackage v1, EPackage v2) {
        int removed = 0;
        Map<String, EClass> v2map = classMap(v2);
        for (EClass c1 : getClasses(v1)) {
            EClass c2 = v2map.get(c1.getName());
            if (c2 == null) continue;
            Set<String> a2 = attrNames(c2);
            for (EAttribute a : c1.getEAttributes())
                if (!a2.contains(a.getName())) removed++;
        }
        return removed;
    }

    public static int countTypeChanges(EPackage v1, EPackage v2) {
        int changes = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            Map<String, String> t1 = attrTypes(c1);
            for (EAttribute a2 : c2.getEAttributes()) {
                String oldType = t1.get(a2.getName());
                if (oldType != null && a2.getEType() != null
                        && !oldType.equals(a2.getEType().getName()))
                    changes++;
            }
        }
        return changes;
    }

    public static int countReferences(EPackage pkg) {
        return getClasses(pkg).stream()
                .mapToInt(c -> c.getEReferences().size()).sum();
    }

    public static int countAddedReferences(EPackage v1, EPackage v2) {
        int added = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            Set<String> r1 = refNames(c1);
            for (EReference r : c2.getEReferences())
                if (!r1.contains(r.getName())) added++;
        }
        return added;
    }

    public static int countRemovedReferences(EPackage v1, EPackage v2) {
        int removed = 0;
        Map<String, EClass> v2map = classMap(v2);
        for (EClass c1 : getClasses(v1)) {
            EClass c2 = v2map.get(c1.getName());
            if (c2 == null) continue;
            Set<String> r2 = refNames(c2);
            for (EReference r : c1.getEReferences())
                if (!r2.contains(r.getName())) removed++;
        }
        return removed;
    }

    public static int countMultiplicityChanges(EPackage v1, EPackage v2) {
        int changes = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            Map<String, int[]> m1 = refMultiplicity(c1);
            for (EReference r2 : c2.getEReferences()) {
                int[] old = m1.get(r2.getName());
                if (old != null
                        && (old[0] != r2.getLowerBound()
                         || old[1] != r2.getUpperBound()))
                    changes++;
            }
        }
        return changes;
    }

    public static int countContainmentChanges(EPackage v1, EPackage v2) {
        int changes = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            Map<String, Boolean> cont1 = refContainment(c1);
            for (EReference r2 : c2.getEReferences()) {
                Boolean old = cont1.get(r2.getName());
                if (old != null && !old.equals(r2.isContainment()))
                    changes++;
            }
        }
        return changes;
    }

    public static int countAbstractChanges(EPackage v1, EPackage v2) {
        int changes = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 != null && c1.isAbstract() != c2.isAbstract())
                changes++;
        }
        return changes;
    }

    public static int countSuperTypeChanges(EPackage v1, EPackage v2) {
        int changes = 0;
        Map<String, EClass> v1map = classMap(v1);
        for (EClass c2 : getClasses(v2)) {
            EClass c1 = v1map.get(c2.getName());
            if (c1 == null) continue;
            if (!superTypes(c1).equals(superTypes(c2))) changes++;
        }
        return changes;
    }

    public static int nsUriChanged(EPackage v1, EPackage v2) {
        String u1 = v1.getNsURI() != null ? v1.getNsURI() : "";
        String u2 = v2.getNsURI() != null ? v2.getNsURI() : "";
        return u1.equals(u2) ? 0 : 1;
    }

    public static Map<String, EClass> classMap(EPackage pkg) {
        Map<String, EClass> m = new HashMap<>();
        for (EClass c : getClasses(pkg)) m.put(c.getName(), c);
        return m;
    }

    private static Set<String> attrNames(EClass c) {
        return c.getEAttributes().stream()
                .map(EAttribute::getName).collect(Collectors.toSet());
    }

    private static Set<String> refNames(EClass c) {
        return c.getEReferences().stream()
                .map(EReference::getName).collect(Collectors.toSet());
    }

    private static Map<String, String> attrTypes(EClass c) {
        Map<String, String> m = new HashMap<>();
        for (EAttribute a : c.getEAttributes())
            if (a.getEType() != null) m.put(a.getName(), a.getEType().getName());
        return m;
    }

    private static Map<String, int[]> refMultiplicity(EClass c) {
        Map<String, int[]> m = new HashMap<>();
        for (EReference r : c.getEReferences())
            m.put(r.getName(), new int[]{r.getLowerBound(), r.getUpperBound()});
        return m;
    }

    private static Map<String, Boolean> refContainment(EClass c) {
        Map<String, Boolean> m = new HashMap<>();
        for (EReference r : c.getEReferences())
            m.put(r.getName(), r.isContainment());
        return m;
    }

    private static Set<String> superTypes(EClass c) {
        return c.getESuperTypes().stream()
                .map(EClass::getName).collect(Collectors.toSet());
    }
}
