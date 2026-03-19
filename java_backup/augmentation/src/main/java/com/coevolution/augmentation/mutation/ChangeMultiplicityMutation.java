
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class ChangeMultiplicityMutation implements MutationOperator {
    @Override public String getChangeType() { return "EREFERENCE_MULTIPLICITY_CHANGED"; }

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
                    int oldUpper = ref.getUpperBound();
                    int oldLower = ref.getLowerBound();
                    int newUpper = (oldUpper == -1) ? 1 : -1;
                    int newLower = (newUpper == -1) ? 0 : 1;
                    ref.setUpperBound(newUpper);
                    ref.setLowerBound(newLower);
                    return MutationResult.success(
                            "EREFERENCE_MULTIPLICITY_CHANGED",
                            ref.getName(), cls.getName(),
                            "[" + oldLower + ".." + (oldUpper==-1?"*":oldUpper) + "]",
                            "[" + newLower + ".." + (newUpper==-1?"*":newUpper) + "]",
                            "Changed multiplicity of " + ref.getName());
                }
            }
        }
        return MutationResult.failure("No reference found");
    }
}