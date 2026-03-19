
package com.coevolution.core.model;

import java.util.List;

/**
 * Result of a model validation.
 */
public class ValidationResult {

    private final boolean      valid;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(boolean valid,
                             List<String> errors,
                             List<String> warnings) {
        this.valid    = valid;
        this.errors   = errors;
        this.warnings = warnings;
    }

    public boolean      isValid()     { return valid;    }
    public List<String> getErrors()   { return errors;   }
    public List<String> getWarnings() { return warnings; }
    public int getErrorCount()        { return errors.size();   }
    public int getWarningCount()      { return warnings.size(); }

    public String getSummary() {
        return (valid ? "✅ VALID" : "❌ INVALID")
            + " | errors="   + errors.size()
            + " | warnings=" + warnings.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{\n");
        sb.append("  valid=").append(valid).append("\n");
        sb.append("  errors=").append(errors.size()).append("\n");
        for (String e : errors)
            sb.append("    → ").append(e).append("\n");
        sb.append("  warnings=")
          .append(warnings.size()).append("\n");
        for (String w : warnings)
            sb.append("    → ").append(w).append("\n");
        sb.append("}");
        return sb.toString();
    }
}