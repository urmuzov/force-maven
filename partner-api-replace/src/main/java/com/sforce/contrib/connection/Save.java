package com.sforce.contrib.connection;

import com.sforce.contrib.partner.SObject;
import com.sforce.soap.partner.SaveResult;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:06
*/
public class Save {
    public final SObject object;
    public final SaveResult result;
    public final com.sforce.soap.partner.sobject.SObject original;

    public Save(SObject object, SaveResult result, com.sforce.soap.partner.sobject.SObject original) {
        this.object = object;
        this.result = result;
        this.original = original;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Save{");
        sb.append("object=").append(object);
        sb.append(", result=").append(result);
        sb.append(", original=").append(original);
        sb.append('}');
        return sb.toString();
    }
}
