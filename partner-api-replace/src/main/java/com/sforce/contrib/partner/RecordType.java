package com.sforce.contrib.partner;

import java.util.Calendar;

public class RecordType extends SObjectType {
    private Field<String> businessProcessId;
    private Field<String> description;
    private Field<String> developerName;
    private Field<Boolean> isActive;
    private Field<String> namespacePrefix;
    private Field<String> sObjectType;
    private Field<Calendar> systemModstamp;

    public void init(Package pkg) {
        super.init(pkg, "RecordType");
        this.businessProcessId = new Field<String>(this, "businessProcessId", "BusinessProcessId", FieldType.LOOKUP, null, null, null, false, false);
        addField(businessProcessId);
        this.description = new Field<String>(this, "description", "Description", FieldType.TEXT, null, null, null, false, false);
        addField(description);
        this.developerName = new Field<String>(this, "developerName", "DeveloperName", FieldType.TEXT, null, null, null, false, false);
        addField(developerName);
        this.isActive = new Field<Boolean>(this, "isActive", "IsActive", FieldType.CHECKBOX, null, null, null, false, false);
        addField(isActive);
        this.namespacePrefix = new Field<String>(this, "namespacePrefix", "NamespacePrefix", FieldType.TEXT, null, null, null, false, false);
        addField(namespacePrefix);
        this.sObjectType = new Field<String>(this, "sObjectType", "SobjectType", FieldType.TEXT, null, null, null, false, false);
        addField(sObjectType);
        this.systemModstamp = new Field<Calendar>(this, "systemModstamp", "SystemModstamp", FieldType.DATE_TIME, null, null, null, false, false);
        addField(systemModstamp);
    }

    public Field<String> businessProcessId() {
        return businessProcessId;
    }

    public Field<String> description() {
        return description;
    }

    public Field<String> developerName() {
        return developerName;
    }

    public Field<Boolean> isActive() {
        return isActive;
    }

    public Field<String> namespacePrefix() {
        return namespacePrefix;
    }

    public Field<String> sObjectType() {
        return sObjectType;
    }

    public Field<Calendar> systemModstamp() {
        return systemModstamp;
    }
}
