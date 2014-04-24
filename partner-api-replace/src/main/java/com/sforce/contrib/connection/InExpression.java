package com.sforce.contrib.connection;

import com.google.common.base.Joiner;
import com.sforce.contrib.partner.Field;

import java.util.Collection;

public class InExpression<T> extends Expression<T> {

    protected InExpression(String field, Collection<String> rawValues, boolean notIn) {
        super(new Soql.FieldName<T>(field), (notIn ? "NOT " : "") + "IN", "(" + Joiner.on(", ").join(rawValues) + ")");
    }

//    protected InExpression(String field, Soql builder, boolean notIn) {
//        super(new Soql.FieldName<T>(field), (notIn ? "NOT " : "") + "IN", builder.toString(true));
//    }

    protected InExpression(Field<T> field, Collection<String> rawValues, boolean notIn) {
        super(new Soql.FieldName<T>(field), (notIn ? "NOT " : "") + "IN", "(" + Joiner.on(", ").join(rawValues) + ")");
    }

//    protected InExpression(Field<T> field, Soql builder, boolean notIn) {
//        super(new Soql.FieldName<T>(field), (notIn ? "NOT " : "") + "IN", builder.toString(true));
//    }
}