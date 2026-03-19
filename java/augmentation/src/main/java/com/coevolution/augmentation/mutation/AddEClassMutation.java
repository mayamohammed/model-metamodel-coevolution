
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.*;

public class AddEClassMutation implements MutationOperator {
    private static int counter = 0;
    private static final String[] NAMES = {
        "GeneratedEntity","AugmentedNode","SyntheticElement",
        "DerivedComponent","ExtendedObject","NewConcept",
        "AddedType","ComputedClass","InjectedEntity","VirtualNode"
    };

    @Override public String getChangeType() { return "ECLASS_ADDED"; }
    @Override public boolean canApply(EPackage pkg) { return true; }

    @Override
    public MutationResult apply(EPackage pkg) {
        EClass newClass = EcoreFactory.eINSTANCE.createEClass();
        String name = NAMES[counter % NAMES.length] + "_" + counter++;
        newClass.setName(name);
        EAttribute attr = EcoreFactory.eINSTANCE.createEAttribute();
        attr.setName("id");
        attr.setEType(EcorePackage.Literals.ESTRING);
        newClass.getEStructuralFeatures().add(attr);
        pkg.getEClassifiers().add(newClass);
        return MutationResult.success("ECLASS_ADDED", name,
                pkg.getName(), null, name, "Added EClass " + name);
    }
}
