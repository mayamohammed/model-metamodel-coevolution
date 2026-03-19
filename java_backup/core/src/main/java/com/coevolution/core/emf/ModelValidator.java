package com.coevolution.core.emf;

import com.coevolution.core.model.ValidationResult;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates XMI models against their metamodel.
 */
public class ModelValidator {

    /**
     * Validates a single model resource.
     */
    public ValidationResult validate(Resource resource,
                                      EPackage ePackage) {
        List<String> errors   = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (resource == null) {
            errors.add("Resource is null");
            return new ValidationResult(
                false, errors, warnings
            );
        }
        if (resource.getContents().isEmpty()) {
            errors.add("Resource is empty");
            return new ValidationResult(
                false, errors, warnings
            );
        }

        for (EObject obj : resource.getContents()) {
            Diagnostic diag =
                Diagnostician.INSTANCE.validate(obj);

            if (diag.getSeverity() == Diagnostic.ERROR) {
                for (Diagnostic child : diag.getChildren()) {
                    if (child.getSeverity()
                            == Diagnostic.ERROR) {
                        errors.add(
                            "[ERROR] " + child.getMessage()
                        );
                    }
                    if (child.getSeverity()
                            == Diagnostic.WARNING) {
                        warnings.add(
                            "[WARNING] " + child.getMessage()
                        );
                    }
                }
            }
        }

        boolean isValid = errors.isEmpty();
        System.out.println(
            "[ModelValidator] "
            + (isValid ? "✅ VALID" : "❌ INVALID")
            + " errors=" + errors.size()
        );
        return new ValidationResult(
            isValid, errors, warnings
        );
    }

    /**
     * Validates all models in a list.
     */
    public List<ValidationResult> validateAll(
            List<Resource> resources,
            EPackage ePackage) {

        List<ValidationResult> results = new ArrayList<>();
        int valid = 0, invalid = 0;

        for (Resource r : resources) {
            ValidationResult res = validate(r, ePackage);
            results.add(res);
            if (res.isValid()) valid++;
            else invalid++;
        }

        System.out.println(
            "[ModelValidator] "
            + valid + " valid, "
            + invalid + " invalid"
        );
        return results;
    }

    /**
     * Quick check — returns true if valid.
     */
    public boolean isValid(Resource resource,
                            EPackage ePackage) {
        return validate(resource, ePackage).isValid();
    }
}