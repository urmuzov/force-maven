package com.sforce.contrib.connection;

import com.sforce.soap.partner.DeleteResult;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:06
*/
public class Delete {
    public final String objectId;
    public final DeleteResult result;

    public Delete(String objectId, DeleteResult result) {
        this.objectId = objectId;
        this.result = result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Delete{");
        sb.append("objectId='").append(objectId).append('\'');
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
