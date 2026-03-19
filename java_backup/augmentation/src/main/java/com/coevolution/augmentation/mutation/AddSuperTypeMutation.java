
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;
import java.util.List;
import java.util.stream.Collectors;

public class AddSuperTypeMutation implements MutationOperator {
    @Override public String getChangeType() { return "ECLASS_SUPERTYPE_ADDED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        List<EClass> classes = pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c).collect(Collectors.toList());
        return classes.size() >= 2
                && classes.stream().anyMatch(EClass::isAbstract);
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        List<EClass> classes = pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c).collect(Collectors.toList());
        EClass parent = classes.stream()
                .filter(EClass::isAbstract).findFirst().orElse(null);
        EClass child  = classes.stream()
                .filter(c -> !c.isAbstract()
                          && (parent == null || !c.getESuperTypes().contains(parent)))
                .findFirst().orElse(null);
        if (parent == null || child == null)
            return MutationResult.failure("No suitable parent/child");
        child.getESuperTypes().add(parent);
        return MutationResult.success("ECLASS_SUPERTYPE_ADDED",
                parent.getName(), child.getName(), null, parent.getName(),
                child.getName() + " now extends " + parent.getName());
    }
}