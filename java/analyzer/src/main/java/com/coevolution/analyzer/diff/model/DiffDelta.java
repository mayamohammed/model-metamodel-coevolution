package com.coevolution.analyzer.diff.model;

public class DiffDelta {
    public enum Kind { ADD, DELETE, CHANGE, RENAME }
    public enum ElementType { PACKAGE, CLASS, ATTRIBUTE, REFERENCE }

    private Kind kind;
    private ElementType elementType;
    private String element;
    private String details;

    public DiffDelta(Kind kind, ElementType elementType, String element, String details) {
        this.kind = kind; this.elementType = elementType;
        this.element = element; this.details = details;
    }
    public Kind getKind()              { return kind; }
    public ElementType getElementType(){ return elementType; }
    public String getElement()         { return element; }
    public String getDetails()         { return details; }
}