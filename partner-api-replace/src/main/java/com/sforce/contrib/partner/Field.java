package com.sforce.contrib.partner;

import java.util.Map;

public class Field<T> {
    private static Map<String, Class> objectClassesMap;

    private final SObjectType sObjectType;
    private final String javaName;
    private final String sfName;
    private final String label;
    private final FieldType type;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final boolean custom;
    private final boolean removeOnSave;
    private final FieldReference reference;

    public Field(SObjectType sObjectType, String javaName, String sfName, String label, FieldType type, Integer length, Integer precision, Integer scale, boolean custom, boolean removeOnSave) {
        this(sObjectType, javaName, sfName, label, type, length, precision, scale, custom, removeOnSave, null);
    }

    public Field(SObjectType sObjectType, String javaName, String sfName, String label, FieldType type, Integer length, Integer precision, Integer scale, boolean custom, boolean removeOnSave, FieldReference reference) {
        this.sObjectType = sObjectType;
        this.javaName = javaName;
        this.sfName = sfName;
        this.label = label;
        this.type = type;
        this.length = length;
        this.precision = precision;
        this.scale = scale;
        this.custom = custom;
        this.removeOnSave = removeOnSave;
        this.reference = reference;
    }

    public FieldReference reference() {
        return this.reference;
    }

    public String javaName() {
        return javaName;
    }

    public String apiName(Context context) {
        if (!custom) {
            return sfName;
        } else {
            String namespace = context.get(sObjectType.pkg());
            return Context.withNamespace(namespace, sfName);
        }
    }

    public String sfName() {
        return sfName;
    }

    public String label() {
        return label;
    }

    public FieldType type() {
        return type;
    }

    public Integer length() {
        return length;
    }

    public Integer precision() {
        return precision;
    }

    public Integer scale() {
        return scale;
    }

    public boolean custom() {
        return custom;
    }

    public boolean removeOnSave() {
        return removeOnSave;
    }

    @Override
    public String toString() {
        return sfName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Field)) return false;

        Field field = (Field) o;

        if (sObjectType != null ? !sObjectType.equals(field.sObjectType) : field.sObjectType != null) return false;
        if (sfName != null ? !sfName.equals(field.sfName) : field.sfName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sObjectType != null ? sObjectType.hashCode() : 0;
        result = 31 * result + (sfName != null ? sfName.hashCode() : 0);
        return result;
    }
}
