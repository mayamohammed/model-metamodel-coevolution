
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class ChangeAbstractMutation implements MutationOperator {
    @Override public String getChangeType() { return "ECLASS_ABSTRACT_CHANGED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream().anyMatch(c -> c instanceof EClass);
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        for (EClassifier ec : pkg.getEClassifiers()) {
            if (ec instanceof EClass) {
                EClass cls = (EClass) ec;
                boolean old = cls.isAbstract();
                cls.setAbstract(!old);
                return MutationResult.success("ECLASS_ABSTRACT_CHANGED",
                        cls.getName(), pkg.getName(),
                        String.valueOf(old), String.valueOf(!old),
                        "Changed abstract of " + cls.getName()
                                + " : " + old + " -> " + !old);
            }
        }
        return MutationResult.failure("No class found");
    }
}
