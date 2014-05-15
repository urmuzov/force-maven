package com.sforce.contrib.connection;

import com.sforce.soap.partner.DeleteResult;

import java.util.List;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:06
*/
public class Delete extends RuntimeException {
    private String objectId;
    private String id;
    private List<Error> errors;
    private boolean success;

    public Delete(String objectId, DeleteResult result) {
        super(result.toString());
        this.objectId = objectId;
        this.id = result.getId();
        this.errors = Error.convert(result.getErrors());
        this.success = result.isSuccess();
    }

    public String getObjectId() {
        return objectId;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Delete{");
        sb.append("objectId='").append(objectId).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", errors=").append(errors);
        sb.append(", success=").append(success);
        sb.append('}');
        return sb.toString();
    }
}
