package com.coevolution.augmentation.generator;

import com.coevolution.augmentation.mutation.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.File;
import java.util.*;

public class EcoreMutationEngine {

    private final List<MutationOperator> operators;

    public EcoreMutationEngine() {
        Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .put("ecore", new XMIResourceFactoryImpl());
        EPackage.Registry.INSTANCE
                .put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

        operators = new ArrayList<>();
        operators.add(new AddEClassMutation());
        operators.add(new RemoveEClassMutation());
        operators.add(new AddAttributeMutation());
        operators.add(new RemoveAttributeMutation());
        operators.add(new ChangeTypeMutation());
        operators.add(new AddReferenceMutation());
        operators.add(new RemoveReferenceMutation());
        operators.add(new ChangeMultiplicityMutation());
        operators.add(new ChangeAbstractMutation());
        operators.add(new AddSuperTypeMutation());
    }

    public List<MutationCandidate> generateMutations(File sourceFile) {
        List<MutationCandidate> list = new ArrayList<>();
        String absPath = sourceFile.getAbsolutePath();

        for (MutationOperator op : operators) {
            try {
                ResourceSet rs1 = newResourceSet();
                Resource origRes = rs1.getResource(URI.createFileURI(absPath), true);
                if (origRes.getContents().isEmpty()) continue;
                EPackage original = (EPackage) origRes.getContents().get(0);

                if (!op.canApply(original)) continue;

                ResourceSet rs2 = newResourceSet();
                Resource cloneRes = rs2.getResource(URI.createFileURI(absPath), true);
                if (cloneRes.getContents().isEmpty()) continue;
                EPackage clone = (EPackage) cloneRes.getContents().get(0);

                MutationResult r = op.apply(clone);
                if (r != null && r.isSuccess()) {
                    list.add(new MutationCandidate(op, original, clone, r));
                }
            } catch (Exception e) {
                System.err.println("[WARN] Mutation ignoree ["
                        + op.getClass().getSimpleName() + "] sur "
                        + sourceFile.getName() + " : " + e.getMessage());
            }
        }
        return list;
    }

    public List<MutationCandidate> generateMutations(EPackage original) {
        List<MutationCandidate> list = new ArrayList<>();
        for (MutationOperator op : operators) {
            try {
                if (!op.canApply(original)) continue;
                EPackage clone = EcoreUtil.copy(original);
                MutationResult r = op.apply(clone);
                if (r != null && r.isSuccess()) {
                    list.add(new MutationCandidate(op, original, clone, r));
                }
            } catch (Exception e) {
                System.err.println("[WARN] Mutation ignoree ["
                        + op.getClass().getSimpleName() + "] : " + e.getMessage());
            }
        }
        return list;
    }

    public EPackage loadEcore(File f) throws Exception {
        ResourceSet rs = newResourceSet();
        Resource res = rs.getResource(URI.createFileURI(f.getAbsolutePath()), true);
        if (res.getContents().isEmpty())
            throw new IllegalStateException("Fichier vide ou invalide : " + f.getName());
        return (EPackage) res.getContents().get(0);
    }

    public void saveEcore(EPackage pkg, File out) throws Exception {
        if (pkg == null) throw new IllegalArgumentException("pkg null");

        if (pkg.eResource() != null) {
            pkg.eResource().getContents().remove(pkg);
        }

        out.getParentFile().mkdirs();
        ResourceSet rs = newResourceSet();
        Resource res = rs.createResource(URI.createFileURI(out.getAbsolutePath()));
        res.getContents().add(pkg);

        // ✅ FIX FINAL : supprime silencieusement les dangling refs au save
        Map<String, Object> options = new HashMap<>();
        options.put(XMIResource.OPTION_PROCESS_DANGLING_HREF,
                    XMIResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
        res.save(options);
    }

    private ResourceSet newResourceSet() {
        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry()
          .getExtensionToFactoryMap()
          .put("ecore", new XMIResourceFactoryImpl());
        rs.getPackageRegistry()
          .put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        return rs;
    }

    public static class MutationCandidate {
        public final MutationOperator operator;
        public final EPackage         originalPackage;
        public final EPackage         mutatedPackage;
        public final MutationResult   result;

        public MutationCandidate(MutationOperator op,
                                  EPackage original,
                                  EPackage mutated,
                                  MutationResult r) {
            this.operator        = op;
            this.originalPackage = original;
            this.mutatedPackage  = mutated;
            this.result          = r;
        }
    }
}
