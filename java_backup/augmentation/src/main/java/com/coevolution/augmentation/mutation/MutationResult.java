
package com.coevolution.augmentation.mutation;

public class MutationResult {
    private final String changeType;
    private final String element;
    private final String context;
    private final String oldValue;
    private final String newValue;
    private final String description;
    private final boolean success;

    public MutationResult(String changeType, String element, String context,
                          String oldValue, String newValue,
                          String description, boolean success) {
        this.changeType  = changeType;
        this.element     = element;
        this.context     = context;
        this.oldValue    = oldValue;
        this.newValue    = newValue;
        this.description = description;
        this.success     = success;
    }

    public static MutationResult success(String changeType, String element,
                                         String context, String oldValue,
                                         String newValue, String description) {
        return new MutationResult(changeType, element, context,
                                  oldValue, newValue, description, true);
    }

    public static MutationResult failure(String reason) {
        return new MutationResult("NONE","","","","",reason, false);
    }

    public String  getChangeType()  { return changeType;  }
    public String  getElement()     { return element;     }
    public String  getContext()     { return context;     }
    public String  getOldValue()    { return oldValue;    }
    public String  getNewValue()    { return newValue;    }
    public String  getDescription() { return description; }
    public boolean isSuccess()      { return success;     }
}