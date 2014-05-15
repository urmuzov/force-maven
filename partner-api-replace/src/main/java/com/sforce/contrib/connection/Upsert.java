package com.sforce.contrib.connection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sforce.contrib.partner.SObject;
import com.sforce.soap.partner.UpsertResult;

import java.util.List;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:06
*/
public class Upsert extends RuntimeException {
    private SObject object;
    private String id;
    private List<Error> errors;
    private boolean success;
    private boolean created;
    private transient final com.sforce.soap.partner.sobject.SObject original;

    public Upsert(SObject object, UpsertResult result, com.sforce.soap.partner.sobject.SObject original) {
        super(result.toString());
        this.object = object;
        this.id = result.getId();
        this.errors = Error.convert(result.getErrors());
        this.success = result.isSuccess();
        this.created = result.isCreated();
        this.original = original;
    }

    public SObject getObject() {
        return object;
    }

    public String getId() {
        return id;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCreated() {
        return created;
    }

    @JsonIgnore
    public com.sforce.soap.partner.sobject.SObject getOriginal() {
        return original;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Upsert{");
        sb.append("object=").append(object);
        sb.append(", id='").append(id).append('\'');
        sb.append(", errors=").append(errors);
        sb.append(", success=").append(success);
        sb.append(", created=").append(created);
        sb.append(", original=").append(original);
        sb.append('}');
        return sb.toString();
    }
}
