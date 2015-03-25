package com.sforce.contrib.partner;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sforce.ws.bind.XmlObject;
import org.joda.time.*;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: urmuzov
 * Date: 14.04.14
 * Time: 15:56
 */
public class SObject {

    private static class NameParser {
        private String namespace;
        private String name;
        private boolean custom;

        public NameParser(String fullName) {
            String[] parts = fullName.split("__");
            if (parts.length == 1) {
                namespace = null;
                name = parts[0];
                custom = false;
            } else if (parts.length == 2) {
                namespace = null;
                name = parts[0];
                custom = true;
            } else if (parts.length == 3) {
                namespace = parts[0];
                name = parts[1];
                custom = true;
            } else {
                throw new IllegalArgumentException("Can't parse '" + fullName + "'");
            }
        }

        public String withoutNamespace() {
            return custom ? name + "__c" : name;
        }
    }

    private static Object convertIfSObject(XmlObject child) {
        if (child.getXmlType() != null && "sObject".equals(child.getXmlType().getLocalPart())) {
            return new SObject(child);
        }
        return child.getValue();
    }

    public static String convert15to18(String id) {
        if (id == null) {
            return null;
        } else if (id.length() == 18) {
            return id;
        } else if (id.length() != 15) {
            return id;
        } else {
            String suffix = "";
            for (int i = 0; i < 3; i++) {
                int flags = 0;
                for (int j = 0; j < 5; j++) {
                    String c = id.substring(i * 5 + j, i * 5 + j + 1);
                    if ((c.compareTo("A") >= 0) && (c.compareTo("Z") <= 0)) {
                        flags += 1 << j;
                    }
                }
                suffix = suffix + "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".substring(flags, flags + 1);
            }
            return id + suffix;
        }
    }

    public static SObject merge(Collection<SObject> sObjects) {
        if (sObjects == null || sObjects.isEmpty()) {
            return null;
        }
        if (sObjects.size() == 1) {
            return sObjects.iterator().next();
        }
        String id = null;
        SObjectType type = null;
        String typeString = null;
        Map<Field, Object> fields = Maps.newHashMap();
        Map<String, Object> unrecognizedFields = Maps.newHashMap();
        for (SObject sObject : sObjects) {
            if (id == null) {
                id = sObject.getId();
            } else if (sObject.getId() == null || id.equals(sObject.getId())) {
                //ничего не делаем
            } else {
                throw new IllegalArgumentException("Can't merge two different ids: '" + id + "' and '" + sObject.getId() + "'");
            }
            if (type == null) {
                type = sObject.getType();
            } else if (sObject.getType() == null || type.equals(sObject.getType())) {
                //ничего не делаем
            } else {
                throw new IllegalArgumentException("Can't merge two different types: '" + type + "' and '" + sObject.getType() + "'");
            }
            if (typeString == null) {
                typeString = sObject.getTypeString();
            } else if (sObject.getTypeString() == null || typeString.equals(sObject.getTypeString())) {
                //ничего не делаем
            } else {
                throw new IllegalArgumentException("Can't merge two different typeStrings: '" + typeString + "' and '" + sObject.getTypeString() + "'");
            }
            fields.putAll(sObject.getFields());
            unrecognizedFields.putAll(sObject.getUnrecognizedFields());
        }
        SObject out = new SObject(type);
        out.id = id;
        out.typeString = typeString;
        out.fields = fields;
        out.unrecognizedFields = unrecognizedFields;
        return out;
    }

    private String id;
    private SObjectType type;
    private String typeString;
    private Map<Field, Object> fields = Maps.newHashMap();
    private Map<String, Object> unrecognizedFields = Maps.newHashMap();

    public SObject(SObjectType type) {
        this.type = type;
    }

    public SObject(String typeString) {
        this.typeString = typeString;
    }

    public SObject(XmlObject in) {
        this.type = null;
        Iterator<XmlObject> children = in.getChildren();
        while (children.hasNext()) {
            XmlObject child = children.next();
            String fieldName = child.getName().getLocalPart();
            Object value = child.getValue();
            if (fieldName.equalsIgnoreCase("Id")) {
                id = (String) value;
            } else if (fieldName.equalsIgnoreCase("Type")) {
                typeString = (String) value;
            } else {
                unrecognizedFields.put(fieldName, convertIfSObject(child));
            }
        }
    }

    public SObject(XmlObject in, SObjectType from) {
        this.type = from;
        Iterator<XmlObject> children = in.getChildren();
        while (children.hasNext()) {
            XmlObject child = children.next();
            String fieldName = child.getName().getLocalPart();
            Object value = child.getValue();
            if (fieldName.equalsIgnoreCase("Id")) {
                id = (String) value;
            } else if (fieldName.equalsIgnoreCase("Type")) {
                //type = (String) value;
            } else {
                NameParser nameParser = new NameParser(fieldName);
                Field fieldMd = from.bySfName(nameParser.withoutNamespace());
                if (fieldMd == null) {
                    unrecognizedFields.put(fieldName, convertIfSObject(child));
                } else {
                    FieldType type = fieldMd.type();
                    if (value == null) {
                        //fields.put(fieldMd, null); не добавляем нулловые поля, потому что при нашей логике при сохранении значение из них удалится
                    } else if (type == FieldType.NUMBER || type == FieldType.PERCENT || type == FieldType.CURRENCY) {
                        if (fieldMd.scale() != null && fieldMd.scale() > 0) {
                            Double v = Double.valueOf(value.toString());
                            fields.put(fieldMd, v);
                        } else {
                            Long v = Double.valueOf(value.toString()).longValue();
                            fields.put(fieldMd, v);
                        }
                    } else if (type == FieldType.CHECKBOX) {
                        Boolean v = Boolean.valueOf(value.toString());
                        fields.put(fieldMd, v);
                    } else if (type == FieldType.DATE_TIME) {
                        DateTime v = DateTime.parse(value.toString(), ISODateTimeFormat.dateTimeParser().withZoneUTC());
                        fields.put(fieldMd, v);
                    } else if (type == FieldType.DATE) {
                        LocalDate v = LocalDate.parse(value.toString());
                        fields.put(fieldMd, v);
                    } else {
                        fields.put(fieldMd, convertIfSObject(child));
                    }
                }
            }
        }
    }

    public com.sforce.soap.partner.sobject.SObject convert(Context context) {
        com.sforce.soap.partner.sobject.SObject out = new com.sforce.soap.partner.sobject.SObject();
        if (id != null) {
            out.setId(id);
        }
        out.setType(type == null ? typeString : type.apiName(context));
        List<String> fieldsToNull = Lists.newArrayList();
        for (Field field : this.fields.keySet()) {
            if (field.removeOnSave()) {
                continue;
            }
            Object val = this.fields.get(field);
            String fieldName = field.apiName(context);
            if (val == null) {
                fieldsToNull.add(fieldName);
            } else if (val instanceof SObject) {
                // пропускаем вложенные SObjects при сохранении
            } else {
                Object value = convertField(val);
                if (value instanceof String && field.length() != null) {
                    String strValue = (String) value;
                    value = strValue.substring(0, Math.min(strValue.length(), field.length()));
                }
                out.setField(fieldName, value);
            }
        }
        for (String fieldName : this.unrecognizedFields.keySet()) {
            Object val = this.unrecognizedFields.get(fieldName);
            if (val == null) {
                fieldsToNull.add(fieldName);
            } else if (val instanceof SObject) {
                // пропускаем вложенные SObjects при сохранении
            } else {
                out.setField(fieldName, convertField(val));
            }
        }
        out.setFieldsToNull(fieldsToNull.toArray(new String[fieldsToNull.size()]));
        return out;
    }

    private Object convertField(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof DateTime) {
            return ((DateTime) value).toGregorianCalendar();
        } else if (value instanceof LocalDate) {
            return ((LocalDate) value).toDateTime(LocalTime.MIDNIGHT, DateTimeZone.UTC).toGregorianCalendar();
        } else if (value instanceof Instant) {
            return ((Instant) value).toDateTime(DateTimeZone.UTC).toGregorianCalendar();
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return value;
        }
    }

    public SObjectType getType() {
        return type;
    }

    public String getTypeString() {
        return typeString;
    }

    public String getId() {
        return id;
    }

    public String getId18() {
        return convert15to18(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public SObject put(String fieldName, Object value) {
        if (fieldName.equalsIgnoreCase("id")) {
            setId((String) value);
            return this;
        }
        unrecognizedFields.put(fieldName, value);
        return this;
    }

    public <T> SObject put(Field<T> field, T value) {
        if (field.sfName().equalsIgnoreCase("id")) {
            setId((String) value);
            return this;
        }
        if (field.type().equals(FieldType.CHECKBOX) && value == null) {
            throw new IllegalArgumentException("Can't set 'null' value to " + FieldType.CHECKBOX + " field '" + field.sfName() + "', use 'true' or 'false' explicitly");
        }
        fields.put(field, value);
        return this;
    }

    public Object find(final String fieldName, final Context context) {
        Field field = Iterables.find(fields.keySet(), new Predicate<Field>() {
            @Override
            public boolean apply(com.sforce.contrib.partner.Field field) {
                return field.apiName(context).equalsIgnoreCase(fieldName);
            }
        }, null);
        if (field != null) {
            return fields.get(field);
        }

        return unrecognizedFields.get(fieldName);
    }

    public Object get(String fieldName) {
        if (fieldName.equalsIgnoreCase("id")) {
            return getId();
        }
        return unrecognizedFields.get(fieldName);
    }

    public <T> T get(Field<T> field) {
        if (field.sfName().equalsIgnoreCase("id")) {
            return (T) getId();
        }
        return (T) fields.get(field);
    }

    public SObject remove(String fieldName) {
        if (fieldName.equalsIgnoreCase("id")) {
            setId(null);
            return this;
        }
        unrecognizedFields.remove(fieldName);
        return this;
    }

    public <T> SObject remove(Field<T> field) {
        if (field.sfName().equalsIgnoreCase("id")) {
            setId(null);
            return this;
        }
        fields.remove(field);
        return this;
    }

    public Map<Field, Object> getFields() {
        return fields;
    }

    public Map<String, Object> getUnrecognizedFields() {
        return unrecognizedFields;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getType() != null ? getType() : "\"" + getTypeString() + "\"");
        sb.append(':');
        sb.append(id);
        sb.append('{');
        for (Map.Entry<Field, Object> e : fields.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append(",");
        }
        for (Map.Entry<String, Object> e : unrecognizedFields.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append(",");
        }
        sb.append('}');
        return sb.toString();
    }
}
