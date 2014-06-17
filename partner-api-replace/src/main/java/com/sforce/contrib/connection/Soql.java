package com.sforce.contrib.connection;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.Field;
import com.sforce.contrib.partner.SObjectType;
import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Soql {

    protected static class FieldName<T> {
        private final String fieldString;
        private final Field<T> fieldType;

        protected FieldName(Field<T> field) {
            this.fieldString = null;
            this.fieldType = field;
        }

        protected FieldName(String field) {
            this.fieldString = field;
            this.fieldType = null;
        }

        public String apiName(Context context) {
            return fieldType != null ? fieldType.apiName(context) : fieldString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldName)) return false;

            FieldName fieldName = (FieldName) o;

            if (fieldString != null ? !fieldString.equals(fieldName.fieldString) : fieldName.fieldString != null) return false;
            if (fieldType != null ? !fieldType.equals(fieldName.fieldType) : fieldName.fieldType != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = fieldString != null ? fieldString.hashCode() : 0;
            result = 31 * result + (fieldType != null ? fieldType.hashCode() : 0);
            return result;
        }
    }

    private static class Aggregate {
        private final String function;
        private final Field field;
        private final String alias;

        private Aggregate(String function, Field field, String alias) {
            this.function = function;
            this.field = field;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Aggregate)) return false;

            Aggregate that = (Aggregate) o;

            if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
            if (field != null ? !field.equals(that.field) : that.field != null) return false;
            if (function != null ? !function.equals(that.function) : that.function != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = function != null ? function.hashCode() : 0;
            result = 31 * result + (field != null ? field.hashCode() : 0);
            result = 31 * result + (alias != null ? alias.hashCode() : 0);
            return result;
        }
    }

    private final Set<Field> selectFields = Sets.newTreeSet(new Comparator<Field>() {
        @Override
        public int compare(Field o1, Field o2) {
            return o1.sfName().compareTo(o2.sfName());
        }
    });
    private final Set<String> selectUnrecognizedFields = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
    private final List<Aggregate> selectAggregates = Lists.newArrayList();
    private final String fromString;
    private final SObjectType fromType;

    private Where whereBuilder;
    private OrderByPair orderBy;
    private Having having;
    private GroupBy groupBy;
    private int limit = Integer.MIN_VALUE;
    private int offset = Integer.MIN_VALUE;

    public Soql(String from) {
        this.fromString = from;
        this.fromType = null;
    }

    public Soql(SObjectType from) {
        this.fromString = null;
        this.fromType = from;
    }

    public static Soql from(SObjectType from) {
        return new Soql(from);
    }

    public static Soql from(String from) {
        return new Soql(from);
    }

    public static <T> Expression expr(Field<T> field, String operation, T value) {
        return exprRaw(field, operation, Expression.serializeValue(value));
    }

    public static Expression expr(String field, String operation, Object value) {
        return exprRaw(field, operation, Expression.serializeValue(value));
    }

    public static <T> Expression exprRaw(Field<T> field, String operation, String rawValue) {
        return new Expression(new FieldName<T>(field), operation, rawValue);
    }

    public static Expression exprRaw(String field, String operation, String rawValue) {
        return new Expression(new FieldName(field), operation, rawValue);
    }

//    public static <T> Expression<T> in(Field<T> field, Soql builder) {
//        return new InExpression<T>(field, builder, false);
//    }

    public static <T> Expression<T> in(Field<T> field, Collection<T> values) {
        return new InExpression<T>(field, InExpression.serializeValues(values), false);
    }

//    public static Expression in(String field, Soql builder) {
//        return new InExpression(field, builder, false);
//    }

    public static <T> Expression in(String field, Collection<T> values) {
        return new InExpression(field, InExpression.serializeValues(values), false);
    }

//    public static <T> Expression<T> notIn(Field<T> field, Soql builder) {
//        return new InExpression<T>(field, builder, true);
//    }

    public static <T> Expression<T> notIn(Field<T> field, Collection<T> values) {
        return new InExpression<T>(field, InExpression.serializeValues(values), true);
    }

//    public static Expression notIn(String field, Soql builder) {
//        return new InExpression(field, builder, true);
//    }

    public static <T> Expression notIn(String field, Collection<T> values) {
        return new InExpression(field, InExpression.serializeValues(values), true);
    }

    public String getFromString() {
        return fromString;
    }

    public SObjectType getFromType() {
        return fromType;
    }

    public Soql select(String field) {
        this.selectUnrecognizedFields.add(field);
        return this;
    }

    public Soql select(String... fields) {
        this.selectUnrecognizedFields.addAll(Arrays.asList(fields));
        return this;
    }

    public Soql select(Field field) {
        this.selectFields.add(field);
        return this;
    }

    public Soql select(Collection<Field> fields) {
        this.selectFields.addAll(fields);
        return this;
    }

    public Soql select(Field... fields) {
        select(Lists.newArrayList(fields));
        return this;
    }

    public Soql selectAggregate(String function, Field field, String alias) {
        selectAggregates.add(new Aggregate(function, field, alias));
        return this;
    }

    public <T> Soql where(Field<T> field, String operation, T value) {
        where(expr(field, operation, value));
        return this;
    }

    public Soql where(String field, String operation, Object value) {
        where(expr(field, operation, value));
        return this;
    }

    public Soql where(Expression expression) {
        where(Where.create(expression));
        return this;
    }

    public Soql where(Where builder) {
        if (this.whereBuilder == null) {
            this.whereBuilder = builder;
        } else {
            and(builder);
        }

        return this;
    }

    public <T> Soql and(Field<T> field, String operation, T value) {
        and(expr(field, operation, value));
        return this;
    }

    public Soql and(String field, String operation, Object value) {
        and(expr(field, operation, value));
        return this;
    }

    public Soql and(Expression expression) {
        and(Where.create(expression));
        return this;
    }

    public Soql and(Where builder) {
        if (this.whereBuilder != null) {
            this.whereBuilder.and(builder);
        } else {
            this.whereBuilder = builder;
        }

        return this;
    }

    public <T> Soql or(Field<T> field, String operation, T value) {
        or(expr(field, operation, value));
        return this;
    }

    public Soql or(String field, String operation, Object value) {
        or(expr(field, operation, value));
        return this;
    }

    public Soql or(Expression expression) {
        or(Where.create(expression));
        return this;
    }

    public Soql or(Where builder) {
        if (this.whereBuilder != null) {
            this.whereBuilder.or(builder);
        } else {
            this.whereBuilder = builder;
        }

        return this;
    }

    public Soql orderBy(Field field) {
        this.orderBy = new OrderByPair(new FieldName(field), "ASC");
        return this;
    }

    public Soql orderBy(String field) {
        this.orderBy = new OrderByPair(new FieldName(field), "ASC");
        return this;
    }

    public Soql orderBy(Field field, String sort) {
        this.orderBy = new OrderByPair(new FieldName(field), sort);
        return this;
    }

    public Soql orderBy(String field, String sort) {
        this.orderBy = new OrderByPair(new FieldName(field), sort);
        return this;
    }

    public Soql groupBy(Field field) {
        this.groupBy = new GroupBy(new FieldName(field));
        return this;
    }

    public Soql groupBy(String field) {
        this.groupBy = new GroupBy(new FieldName(field));
        return this;
    }

    public Soql having(Field field, String operation, Object value) {
        having(expr(field, operation, value));
        return this;
    }

    public Soql having(String field, String operation, Object value) {
        having(expr(field, operation, value));
        return this;
    }

    public Soql having(Expression expression) {
        this.having = new Having(expression);
        return this;
    }

    public Soql limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Soql offset(int offset) {
        this.offset = offset;
        return this;
    }

    public String compile(Context context) {
        return compile(context, false);
    }

    public String compile(Context context, boolean wrap) {
        StringBuilder builder = new StringBuilder();
        if (wrap) {
            builder.append("(");
        }

        builder.append("SELECT");
        if (selectFields.isEmpty() && selectUnrecognizedFields.isEmpty() && selectAggregates.isEmpty()) {
            throw new IllegalStateException("No select fields defined.");
        }

        List<String> fieldNames = Lists.newArrayList();
        for (Field f : selectFields) {
            fieldNames.add(f.apiName(context));
        }
        for (String f : selectUnrecognizedFields) {
            fieldNames.add(f);
        }
        for (Aggregate f : selectAggregates) {
            fieldNames.add(f.function + "(" + f.field.apiName(context) + ") " + f.alias);
        }
        builder.append(" ").append(Joiner.on(", ").join(fieldNames));
        builder.append(" FROM ").append(fromType != null ? fromType.apiName(context) : fromString);

        if (this.whereBuilder != null) {
            builder.append(" WHERE ").append(this.whereBuilder.compile(context));
        }

        if (this.groupBy != null) {
            builder.append(" ").append(this.groupBy.compile(context));
        }

        if (this.having != null) {
            builder.append(" ").append(this.having.compile(context));
        }

        if (this.orderBy != null) {
            builder.append(" ").append(this.orderBy.compile(context));
        }

        if (this.limit != Integer.MIN_VALUE) {
            builder.append(" LIMIT ").append(this.limit);
        }

        if (this.offset != Integer.MIN_VALUE) {
            builder.append(" OFFSET ").append(this.offset);
        }

        if (wrap) {
            builder.append(")");
        }

        return builder.toString();
    }

    private class OrderByPair {

        private final FieldName field;
        private final String sort;

        private OrderByPair(FieldName field, String sort) {
            this.field = field;
            this.sort = sort;
        }

        public String compile(Context context) {
            StringBuilder builder = new StringBuilder();
            builder.append("ORDER BY")
                    .append(" ")
                    .append(this.field.apiName(context))
                    .append(" ")
                    .append(this.sort);

            return builder.toString();
        }
    }

    private class GroupBy {

        private final FieldName field;

        private GroupBy(FieldName field) {
            this.field = field;
        }

        public String compile(Context context) {
            StringBuilder builder = new StringBuilder();
            builder.append("GROUP BY")
                    .append(" ")
                    .append(this.field.apiName(context));

            return builder.toString();
        }
    }

    private class Having {

        private final Expression expression;

        private Having(Expression expression) {
            this.expression = expression;
        }

        public String compile(Context context) {
            StringBuilder builder = new StringBuilder();
            builder.append("HAVING")
                    .append(" ")
                    .append(this.expression.compile(context));

            return builder.toString();
        }
    }
}