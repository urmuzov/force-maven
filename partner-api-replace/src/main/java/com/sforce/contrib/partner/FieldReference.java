package com.sforce.contrib.partner;

public class FieldReference {

//    private final SObjectType to;
    private final FieldReferenceType type;
    private final DeleteConstraint deleteConstraint;

    public FieldReference(/*SObjectType to, */FieldReferenceType type, DeleteConstraint deleteConstraint) {
//        this.to = to;
        this.type = type;
        this.deleteConstraint = deleteConstraint;
    }

//    public SObjectType to() {
//        return this.to;
//    }

    public FieldReferenceType type() {
        return this.type;
    }

    public DeleteConstraint deleteConstrain() {
        return this.deleteConstraint;
    }
}
