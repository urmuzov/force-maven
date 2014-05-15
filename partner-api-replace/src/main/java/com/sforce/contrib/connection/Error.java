package com.sforce.contrib.connection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sforce.soap.partner.StatusCode;

import java.util.List;

/**
 * User: urmuzov
 * Date: 14.05.14
 * Time: 16:37
 */
public class Error {
    public static List<Error> convert(com.sforce.soap.partner.Error[] errors) {
        if (errors == null) {
            return null;
        }
        List<Error> out = Lists.newArrayList();
        for (com.sforce.soap.partner.Error e : errors) {
            Error o = new Error();
            o.setMessage(e.getMessage());
            o.setStatusCode(e.getStatusCode());
            o.setFields(ImmutableList.copyOf(e.getFields()));
            out.add(o);
        }
        return out;
    }

    private String message;
    private StatusCode statusCode;
    private List<String> fields;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
