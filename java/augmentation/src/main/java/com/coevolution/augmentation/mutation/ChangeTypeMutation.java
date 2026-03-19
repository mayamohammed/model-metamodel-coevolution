
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class ChangeTypeMutation implements MutationOperator {
    @Override public String getChangeType() { return "EATTRIBUTE_TYPE_CHANGED"; }

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
                for (EAttribute attr : cls.getEAttributes()) {
                    EClassifier oldType = attr.getEType();
                    EClassifier newType;
                    if      (oldType == EcorePackage.Literals.ESTRING)  newType = EcorePackage.Literals.EINT;
                    else if (oldType == EcorePackage.Literals.EINT)     newType = EcorePackage.Literals.EDOUBLE;
                    else if (oldType == EcorePackage.Literals.EDOUBLE)  newType = EcorePackage.Literals.EFLOAT;
                    else                                                 newType = EcorePackage.Literals.ESTRING;
                    String oldName = oldType != null ? oldType.getName() : "null";
                    attr.setEType(newType);
                    return MutationResult.success("EATTRIBUTE_TYPE_CHANGED",
                            attr.getName(), cls.getName(),
                            oldName, newType.getName(),
                            "Changed type of " + attr.getName()
                                    + " : " + oldName + " -> " + newType.getName());
                }
            }
        }
        return MutationResult.failure("No attribute to change");
    }
}
