
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;
import java.util.List;
import java.util.stream.Collectors;

public class AddAttributeMutation implements MutationOperator {
    private static int counter = 0;
    private static final EClassifier[] TYPES = {
        EcorePackage.Literals.ESTRING,  EcorePackage.Literals.EINT,
        EcorePackage.Literals.EBOOLEAN, EcorePackage.Literals.EDOUBLE,
        EcorePackage.Literals.EFLOAT,   EcorePackage.Literals.ELONG
    };
    private static final String[] NAMES = {
        "generatedField","augmentedValue","syntheticProp",
        "computedField","derivedAttr","injectedValue"
    };

    @Override public String getChangeType() { return "EATTRIBUTE_ADDED"; }

    @Override
    public boolean canApply(EPackage pkg) {
        return pkg.getEClassifiers().stream().anyMatch(c -> c instanceof EClass);
    }

    @Override
    public MutationResult apply(EPackage pkg) {
        List<EClass> classes = pkg.getEClassifiers().stream()
                .filter(c -> c instanceof EClass)
                .map(c -> (EClass) c).collect(Collectors.toList());
        EClass target = classes.get(0);
        EAttribute attr = EcoreFactory.eINSTANCE.createEAttribute();
        String name = NAMES[counter % NAMES.length] + "_" + counter;
        EClassifier type = TYPES[counter % TYPES.length];
        counter++;
        attr.setName(name);
        attr.setEType(type);
        target.getEStructuralFeatures().add(attr);
        return MutationResult.success("EATTRIBUTE_ADDED", name,
                target.getName(), null, type.getName(),
                "Added EAttribute " + name + " to " + target.getName());
    }
}
