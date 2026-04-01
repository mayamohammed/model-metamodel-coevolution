
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;
import java.util.List;
import java.util.stream.Collectors;

public class AddReferenceMutation implements MutationOperator {
    private static int counter = 0;

    @Override public String getChangeType() { return "EREFERENCE_ADDED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass).count() >= 2;
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        List<EClass> classes = pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c).collect(Collectors.toList());
        EClass source = classes.get(0);
        EClass target = classes.get(1);
        EReference ref = EcoreFactory.eINSTANCE.createEReference();
        String name = "generatedRef_" + counter++;
        ref.setName(name);
        ref.setEType(target);
        ref.setUpperBound(-1);
        ref.setContainment(counter % 2 == 0);
        source.getEStructuralFeatures().add(ref);
        return MutationResult.success("EREFERENCE_ADDED", name,
                source.getName(), null, target.getName(),
                "Added EReference " + name + " from "
                        + source.getName() + " to " + target.getName());
    }
}
