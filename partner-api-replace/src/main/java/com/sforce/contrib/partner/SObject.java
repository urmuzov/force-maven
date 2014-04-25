package com.sforce.contrib.partner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sforce.ws.bind.XmlObject;
import org.joda.time.*;
import org.joda.time.format.ISODateTimeFormat;

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

    private String id;
    private SObjectType type;
    private String typeString;
    private Map<Field, Object> fields = Maps.newHashMap();
    private Map<String, Object> unrecognizedFields = Maps.newHashMap();

    public SObject(SObjectType type) {
        this.type = type;
    }

    public SObject(com.sforce.soap.partner.sobject.SObject in, String from) {
        this.type = null;
        this.typeString = from;
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
                unrecognizedFields.put(fieldName, value);
            }
        }
    }

    public SObject(com.sforce.soap.partner.sobject.SObject in, SObjectType from) {
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
                    unrecognizedFields.put(fieldName, value);
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
                        fields.put(fieldMd, value);
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
        out.setType(type.apiName(context));
        List<String> fieldsToNull = Lists.newArrayList();
        for (Field field : this.fields.keySet()) {
            if (field.removeOnSave()) {
                continue;
            }
            Object val = this.fields.get(field);
            String fieldName = field.apiName(context);
            if (val == null) {
                fieldsToNull.add(fieldName);
            } else {
                Object value = convertField(this.fields.get(field));
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
            } else {
                out.setField(fieldName, convertField(this.unrecognizedFields.get(fieldName)));
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SObject put(String fieldName, Object value) {
        unrecognizedFields.put(fieldName, value);
        return this;
    }

    public <T> SObject put(Field<T> field, T value) {
        if (field.type().equals(FieldType.CHECKBOX) && value == null) {
            throw new IllegalArgumentException("Can't set 'null' value to " + FieldType.CHECKBOX + " field '" + field.sfName() + "', use 'true' or 'false' explicitly");
        }
        fields.put(field, value);
        return this;
    }

    public Object get(String fieldName) {
        return unrecognizedFields.get(fieldName);
    }

    public <T> T get(Field<T> field) {
        return (T) fields.get(field);
    }

    public SObject remove(String fieldName) {
        unrecognizedFields.remove(fieldName);
        return this;
    }

    public <T> SObject remove(Field<T> field) {
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
        sb.append(getType());
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
