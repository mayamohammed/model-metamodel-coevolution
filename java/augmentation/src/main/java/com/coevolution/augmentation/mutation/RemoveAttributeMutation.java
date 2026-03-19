
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class RemoveAttributeMutation implements MutationOperator {
    @Override public String getChangeType() { return "EATTRIBUTE_REMOVED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c)
                .anyMatch(cls -> !cls.getEAttributes().isEmpty());
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        for (EClassifier ec : pkg.getEClassifiers()) {
            if (ec instanceof EClass) {
                EClass cls = (EClass) ec;
                if (!cls.getEAttributes().isEmpty()) {
                    EAttribute attr = cls.getEAttributes()
                            .get(cls.getEAttributes().size() - 1);
                    String name = attr.getName();
                    cls.getEStructuralFeatures().remove(attr);
                    return MutationResult.success("EATTRIBUTE_REMOVED",
                            name, cls.getName(), name, null,
                            "Removed EAttribute " + name
                                    + " from " + cls.getName());
                }
            }
        }
        return MutationResult.failure("No attribute to remove");
    }
}
