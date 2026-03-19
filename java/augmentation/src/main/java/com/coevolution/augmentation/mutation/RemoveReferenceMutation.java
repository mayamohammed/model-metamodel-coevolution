
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class RemoveReferenceMutation implements MutationOperator {
    @Override public String getChangeType() { return "EREFERENCE_REMOVED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c)
                .anyMatch(cls -> !cls.getEReferences().isEmpty());
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        for (EClassifier ec : pkg.getEClassifiers()) {
            if (ec instanceof EClass) {
                EClass cls = (EClass) ec;
                if (!cls.getEReferences().isEmpty()) {
                    EReference ref = cls.getEReferences().get(0);
                    String name = ref.getName();
                    cls.getEStructuralFeatures().remove(ref);
                    return MutationResult.success("EREFERENCE_REMOVED",
                            name, cls.getName(), name, null,
                            "Removed EReference " + name
                                    + " from " + cls.getName());
                }
            }
        }
        return MutationResult.failure("No reference to remove");
    }
}