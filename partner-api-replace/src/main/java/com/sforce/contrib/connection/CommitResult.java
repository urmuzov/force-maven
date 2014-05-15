package com.sforce.contrib.connection;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:03
*/
public class CommitResult {
    public final boolean ok;
    public final List<Save> creates;
    public final Map<String, Save> createsById;
    public final List<Save> updates;
    public final Map<String, Save> updatesById;
    public final List<Delete> deletes;
    public final Map<String, Delete> deletesById;

    public CommitResult(List<Save> creates, List<Save> updates, List<Delete> deletes) {
        this.ok = creates.isEmpty() && updates.isEmpty() && deletes.isEmpty();
        this.creates = creates;
        createsById = Maps.newHashMap();
        for (Save s : creates) {
            createsById.put(s.getObject().getId(), s);
        }
        this.updates = updates;
        updatesById = Maps.newHashMap();
        for (Save s : updates) {
            updatesById.put(s.getObject().getId(), s);
        }
        this.deletes = deletes;
        deletesById = Maps.newHashMap();
        for (Delete d : deletes) {
            deletesById.put(d.getObjectId(), d);
        }
    }
}
