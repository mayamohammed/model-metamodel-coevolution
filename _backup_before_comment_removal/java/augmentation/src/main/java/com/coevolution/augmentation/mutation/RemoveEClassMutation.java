
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveEClassMutation implements MutationOperator {
    @Override public String getChangeType() { return "ECLASS_REMOVED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass).count() >= 2;
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        List<EClass> classes = pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c)
                .collect(Collectors.toList());
        EClass toRemove = classes.get(classes.size() - 1);
        String name = toRemove.getName();
        pkg.getEClassifiers().remove(toRemove);
        return MutationResult.success("ECLASS_REMOVED", name,
                pkg.getName(), name, null, "Removed EClass " + name);
    }
}
