package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.Field;

import java.util.List;

public class Where {

    private final String OR_STATEMENT = "OR";
    private final String AND_STATEMENT = "AND";

    private final List<StatementBuilderPair> inners = Lists.newArrayList();
    private final Expression expression;

    public Where(Expression expression) {
        this.expression = expression;
    }

    public static <T> Where create(Field<T> field, String operation, T value) {
        return create(Expression.create(field, operation, value));
    }

    public static Where create(String field, String operation, Object value) {
        return create(Expression.create(field, operation, value));
    }

    public static Where create(Expression expression) {
        return new Where(expression);
    }

    public <T> Where or(Field<T> field, String operation, T value) {
        or(Expression.create(field, operation, value));
        return this;
    }

    public Where or(String field, String operation, Object value) {
        or(Expression.create(field, operation, value));
        return this;
    }

    public Where or(Expression expression) {
        or(new Where(expression));
        return this;
    }

    public Where or(Where builder) {
        this.inners.add(new StatementBuilderPair(OR_STATEMENT, builder));
        return this;
    }

    public <T> Where and(Field<T> field, String operation, T value) {
        and(Expression.create(field, operation, value));
        return this;
    }

    public Where and(String field, String operation, Object value) {
        and(Expression.create(field, operation, value));
        return this;
    }

    public Where and(Expression expression) {
        and(new Where(expression));
        return this;
    }

    public Where and(Where builder) {
        this.inners.add(new StatementBuilderPair(AND_STATEMENT, builder));
        return this;
    }

    public String compile(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        if (inners.size() > 0) {
            builder.append("(");
        }

        builder.append(this.expression.compile(context));
        if (inners.size() > 0) {
            builder.append(")");
        }

        for (StatementBuilderPair inner : inners) {
            builder.append(" ").append(inner.compile(context));
        }

        builder.append(")");

        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + inners.hashCode();
        result = prime * result + expression.hashCode();

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

        Where builder = (Where)o;
        return this.inners.equals(builder.inners)
                && this.expression.equals(builder.expression);

    }

    private class StatementBuilderPair {

        public final String statement;
        public final Where builder;

        public StatementBuilderPair(String statement, Where builder) {
            this.statement = statement;
            this.builder = builder;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + statement.hashCode();
            result = prime * result + builder.hashCode();

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

            StatementBuilderPair pair = (StatementBuilderPair)o;
            return this.statement.equalsIgnoreCase(pair.statement)
                    && this.builder.equals(pair.builder);

        }

        public String compile(Context context) {
            StringBuilder builder = new StringBuilder();
            return builder.append(this.statement)
                    .append(" ")
                    .append(this.builder.compile(context))
                    .toString();
        }
    }
}