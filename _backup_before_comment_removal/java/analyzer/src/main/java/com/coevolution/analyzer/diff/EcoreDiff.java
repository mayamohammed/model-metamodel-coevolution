package com.coevolution.analyzer.diff;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import java.io.File;
import java.util.*;

public class EcoreDiff {
    public static EPackage load(File file) throws Exception {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        Resource r = rs.getResource(URI.createFileURI(file.getAbsolutePath()), true);
        return (EPackage) r.getContents().get(0);
    }
    public static List<EClass> getClasses(EPackage pkg) {
        List<EClass> list = new ArrayList<>();
        for (EClassifier c : pkg.getEClassifiers()) if (c instanceof EClass) list.add((EClass)c);
        return list;
    }
    public static int countAttributes(EPackage pkg) { int n=0; for(EClass c:getClasses(pkg)) n+=c.getEAttributes().size(); return n; }
    public static int countReferences(EPackage pkg) { int n=0; for(EClass c:getClasses(pkg)) n+=c.getEReferences().size(); return n; }
    public static List<EClass> getAddedClasses(EPackage v1,EPackage v2) {
        Set<String> n1=classNames(v1); List<EClass> r=new ArrayList<>();
        for(EClass c:getClasses(v2)) if(!n1.contains(c.getName())) r.add(c); return r;
    }
    public static List<EClass> getRemovedClasses(EPackage v1,EPackage v2) { return getAddedClasses(v2,v1); }
    public static int countAddedAttributes(EPackage v1,EPackage v2) { return countFeatDiff(v1,v2,true); }
    public static int countRemovedAttributes(EPackage v1,EPackage v2) { return countFeatDiff(v2,v1,true); }
    public static int countAddedReferences(EPackage v1,EPackage v2) { return countFeatDiff(v1,v2,false); }
    public static int countRemovedReferences(EPackage v1,EPackage v2) { return countFeatDiff(v2,v1,false); }
    public static int countTypeChanges(EPackage v1,EPackage v2) {
        int n=0; Map<String,EAttribute> a1=attrMap(v1),a2=attrMap(v2);
        for(Map.Entry<String,EAttribute> e:a2.entrySet()){EAttribute o=a1.get(e.getKey());
        if(o!=null&&e.getValue().getEType()!=null&&o.getEType()!=null&&!e.getValue().getEType().getName().equals(o.getEType().getName()))n++;}return n;
    }
    public static int countMultiplicityChanges(EPackage v1,EPackage v2) {
        int n=0; Map<String,EStructuralFeature> f1=featMap(v1),f2=featMap(v2);
        for(Map.Entry<String,EStructuralFeature> e:f2.entrySet()){EStructuralFeature o=f1.get(e.getKey());
        if(o!=null&&(o.getLowerBound()!=e.getValue().getLowerBound()||o.getUpperBound()!=e.getValue().getUpperBound()))n++;}return n;
    }
    public static int countContainmentChanges(EPackage v1,EPackage v2) {
        int n=0; Map<String,EReference> r1=refMap(v1),r2=refMap(v2);
        for(Map.Entry<String,EReference> e:r2.entrySet()){EReference o=r1.get(e.getKey());
        if(o!=null&&o.isContainment()!=e.getValue().isContainment())n++;}return n;
    }
    public static int countAbstractChanges(EPackage v1,EPackage v2) {
        int n=0; Map<String,EClass> c1=classMap(v1),c2=classMap(v2);
        for(Map.Entry<String,EClass> e:c2.entrySet()){EClass o=c1.get(e.getKey());
        if(o!=null&&o.isAbstract()!=e.getValue().isAbstract())n++;}return n;
    }
    public static int countSuperTypeChanges(EPackage v1,EPackage v2) {
        int n=0; Map<String,EClass> c1=classMap(v1),c2=classMap(v2);
        for(Map.Entry<String,EClass> e:c2.entrySet()){EClass o=c1.get(e.getKey());
        if(o!=null&&o.getESuperTypes().size()!=e.getValue().getESuperTypes().size())n++;}return n;
    }
    public static int nsUriChanged(EPackage v1,EPackage v2) {
        String u1=v1.getNsURI(),u2=v2.getNsURI(); return(u1==null?u2!=null:!u1.equals(u2))?1:0;
    }
    private static Set<String> classNames(EPackage p){Set<String> s=new HashSet<>();for(EClass c:getClasses(p))s.add(c.getName());return s;}
    private static Map<String,EClass> classMap(EPackage p){Map<String,EClass> m=new HashMap<>();for(EClass c:getClasses(p))m.put(c.getName(),c);return m;}
    private static Map<String,EAttribute> attrMap(EPackage p){Map<String,EAttribute> m=new HashMap<>();for(EClass c:getClasses(p))for(EAttribute a:c.getEAttributes())m.put(c.getName()+"::"+a.getName(),a);return m;}
    private static Map<String,EStructuralFeature> featMap(EPackage p){Map<String,EStructuralFeature> m=new HashMap<>();for(EClass c:getClasses(p))for(EStructuralFeature f:c.getEStructuralFeatures())m.put(c.getName()+"::"+f.getName(),f);return m;}
    private static Map<String,EReference> refMap(EPackage p){Map<String,EReference> m=new HashMap<>();for(EClass c:getClasses(p))for(EReference r:c.getEReferences())m.put(c.getName()+"::"+r.getName(),r);return m;}
    private static int countFeatDiff(EPackage base,EPackage target,boolean isAttr){
        Set<String> bk=new HashSet<>();
        for(EClass c:getClasses(base))for(EStructuralFeature f:c.getEStructuralFeatures())
            if(isAttr?(f instanceof EAttribute):(f instanceof EReference))bk.add(c.getName()+"::"+f.getName());
        int n=0;
        for(EClass c:getClasses(target))for(EStructuralFeature f:c.getEStructuralFeatures())
            if(isAttr?(f instanceof EAttribute):(f instanceof EReference))if(!bk.contains(c.getName()+"::"+f.getName()))n++;
        return n;
    }
}