
package com.coevolution.augmentation.mutation;
import org.eclipse.emf.ecore.EPackage;

public interface MutationOperator {
    String getChangeType();
    boolean canApply(EPackage pkg);
    MutationResult apply(EPackage pkg);
}