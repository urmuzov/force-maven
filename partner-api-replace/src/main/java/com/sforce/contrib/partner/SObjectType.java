package com.sforce.contrib.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.*;

public abstract class SObjectType {
    private Package pkg;
    private String sfName;
    private boolean custom;
    private List<Field> allFields = new ArrayList<Field>();
    private Map<String, Field> bySfName = new HashMap<String, Field>();
    private Field<String> id;
    private Field<String> name;
    private Field<String> owner;
    private Field<String> createdBy;
    private Field<DateTime> createdDate;
    private Field<String> lastModifiedBy;
    private Field<String> recordTypeId;

    public void init(Package pkg, String sfName) {
        this.pkg = pkg;
        this.sfName = sfName;
        this.custom = sfName.endsWith("__c");
        this.id = new Field<String>(this, "id", "Id", FieldType.ID, null, null, null, false, false);
        addField(id, false);
        this.name = new Field<String>(this, "name", "Name", FieldType.TEXT, 80, null, null, false, false);
        addField(name, false);
        this.owner = new Field<String>(this, "owner", "Owner", FieldType.TEXT, null, null, null, false, false);
        addField(owner, false);
        this.createdBy = new Field<String>(this, "createdBy", "CreatedBy", FieldType.TEXT, null, null, null, false, false);
        addField(createdBy, false);
        this.createdDate = new Field<DateTime>(this, "createdBy", "CreatedDate", FieldType.DATE_TIME, null, null, null, false, false);
        addField(createdDate, false);
        this.lastModifiedBy = new Field<String>(this, "lastModifiedBy", "LastModifiedBy", FieldType.TEXT, null, null, null, false, false);
        addField(lastModifiedBy, false);
        this.recordTypeId = new Field<String>(this, "recordTypeId", "RecordTypeId", FieldType.TEXT, null, null, null, false, false);
        addField(recordTypeId, false);
    }

    public Package pkg() {
        return pkg;
    }

    public List<Field> allFields() {
        return Collections.unmodifiableList(allFields);
    }

    public Field bySfName(String sfName) {
        return bySfName.get(sfName.toLowerCase());
    }

//    public String withNamespace(String name) {
//        return objects.withNamespace(name);
//    }

    public boolean custom() {
        return custom;
    }

    public String apiName(Context context) {
        if (!custom) {
            return sfName;
        } else {
            String namespace = context.get(pkg);
            return Context.withNamespace(namespace, sfName);
        }
    }

    @JsonProperty("sfName")
    public String sfName() {
        return sfName;
    }

    public SObject newSObject() {
        SObject out = new SObject(this);
        return out;
    }

    public Field<String> id() {
        return id;
    }

    public Field<String> name() {
        return name;
    }

    public Field<String> owner() {
        return owner;
    }

    public Field<String> createdBy() {
        return createdBy;
    }

    public Field<DateTime> createdDate() {
        return createdDate;
    }

    public Field<String> lastModifiedBy() {
        return lastModifiedBy;
    }

    public Field<String> recordTypeId() {
        return recordTypeId;
    }

    public NameFieldType nameFieldType() {
        return NameFieldType.UNKNOWN;
    }

    protected void addField(Field field) {
        addField(field, true);
    }

    private void addField(Field field, boolean selfField) {
        allFields.add(field);
        bySfName.put(field.sfName().toLowerCase(), field);
   }

    @Override
    public String toString() {
        return sfName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SObjectType)) return false;

        SObjectType that = (SObjectType) o;

        if (pkg != null ? !pkg.equals(that.pkg) : that.pkg != null) return false;
        if (sfName != null ? !sfName.equals(that.sfName) : that.sfName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pkg != null ? pkg.hashCode() : 0;
        result = 31 * result + (sfName != null ? sfName.hashCode() : 0);
        return result;
    }
}
