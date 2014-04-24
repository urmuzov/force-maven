package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.Field;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Expression<T> {

    private final Soql.FieldName<T> field;
    private final String operation;
    private final String rawValue;

    protected Expression(Soql.FieldName<T> field, String operation, String rawValue) {
        this.field = field;
        this.operation = operation;
        this.rawValue = rawValue;
    }

    public static <T> Expression<T> create(Field<T> field, String operation, T value) {
        return createRaw(field, operation, serializeValue(value));
    }

    public static Expression create(String field, String operation, Object value) {
        return createRaw(field, operation, serializeValue(value));
    }

    public static <T> Expression<T> createRaw(Field<T> field, String operation, String value) {
        return new Expression(new Soql.FieldName(field), operation, value);
    }

    public static Expression createRaw(String field, String operation, String value) {
        return new Expression(new Soql.FieldName(field), operation, value);
    }

    public static <T> Set<String> serializeValues(Collection<T> values) {
        List<String> rawValues = Lists.newArrayList();
        for (Object value : values) {
            rawValues.add(serializeValue(value));
        }

        Set<String> set = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
        set.addAll(Lists.newArrayList(rawValues));

        return set;
    }

    public static String serializeValue(Object value) {
        String rawValue;
        if (value == null) {
            rawValue = "NULL";
        } else if (value instanceof String) {
            rawValue = "'" + value.toString().replaceAll("'", "\\\\'") + "'";
        } else if (value instanceof DateTime) {
            rawValue = (serializeDateTime((DateTime) value));
        } else if (value instanceof Calendar) {
            rawValue = (serializeDateTime(new DateTime(((Calendar) value).getTimeInMillis(), DateTimeZone.UTC)));
        } else {
            rawValue = value.toString();
        }

        return rawValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + field.hashCode();
        result = prime * result + operation.hashCode();
        result = prime * result + rawValue.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        Expression other = (Expression) o;
        return this.field.equals(other.field)
                && this.operation.equalsIgnoreCase(other.operation)
                && this.rawValue.equalsIgnoreCase(other.rawValue);
    }

    public String compile(Context context) {
        StringBuilder builder = new StringBuilder();
        return builder.append(this.field.apiName(context))
                .append(" ")
                .append(this.operation)
                .append(" ")
                .append(this.rawValue)
                .toString();
    }

    private static String serializeDateTime(DateTime value) {
        return value.toString(new DateTimeFormatterBuilder()
                .append(ISODateTimeFormat.date())
                .appendLiteral('T')
                .append(ISODateTimeFormat.hourMinuteSecondMillis())
                .appendLiteral("+0000")
                .toFormatter());
    }
}