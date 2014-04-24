package com.sforce.contrib.partner;

import com.sforce.contrib.connection.Expression;

/**
 * User: urmuzov
 * Date: 7/23/13
 * Time: 1:15 PM
 */
public class WherePart {
    private Expression wherePart;

    public WherePart(Expression wherePart) {
        this.wherePart = wherePart;
    }

    public Expression getWherePart() {
        return wherePart;
    }
}
