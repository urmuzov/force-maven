package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.sforce.contrib.partner.SObject;
import com.sforce.ws.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
* User: urmuzov
* Date: 14.04.14
* Time: 17:06
*/
public class Transaction {
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    private Connection connection;
    private List<SObject> createPool = Lists.newArrayList();
    private List<SObject> updatePool = Lists.newArrayList();
    private List<String> deletePool = Lists.newArrayList();

    protected Transaction(Connection connection) {
        this.connection = connection;
    }

    public void createDeferred(Collection<SObject> objects) {
        createPool.addAll(objects);
    }

    public void updateDeferred(Collection<SObject> objects) {
        updatePool.addAll(objects);
    }

    public void deleteDeferred(Collection<String> ids) {
        deletePool.addAll(ids);
    }

    public void deleteObjectsDeferred(Collection<SObject> objects) {
        if (objects.isEmpty()) {
            return;
        }
        List<String> ids = Lists.newArrayList();
        for (SObject o : objects) {
            ids.add(o.getId());
        }
        deleteDeferred(ids);
    }

    public CommitResult commitDeferred() throws ConnectionException {
        int createSize = createPool.size();
        int updateSize = updatePool.size();
        int deleteSize = deletePool.size();
        return commitDeferred(createSize + updateSize + deleteSize > Connection.SLOW_DOWN_OBJECT_COUNT);
    }

    public CommitResult commitDeferred(boolean slowDown) throws ConnectionException {
        return commitDeferred(slowDown, false);
    }

    public CommitResult commitDeferred(boolean slowDown, boolean exceptionOnFail) throws ConnectionException {
        int createSize = createPool.size();
        int updateSize = updatePool.size();
        int deleteSize = deletePool.size();
        logger.info("Commiting deferred data: creating {}, updating {}, deleteing {}; slowDown: {}; exceptionOnFail: {}", createSize, updateSize, deleteSize, slowDown, exceptionOnFail);

        try {
            List<Delete> deletes = connection.deleteById(deletePool, exceptionOnFail, slowDown);
            List<Save> creates = connection.create(createPool, exceptionOnFail, slowDown);
            List<Save> updates = connection.update(updatePool, exceptionOnFail, slowDown);
            return new CommitResult(creates, updates, deletes);
        } finally {
            deletePool.clear();
            createPool.clear();
            updatePool.clear();
        }
    }

    public int getCreateDeferredSize() {
        return createPool.size();
    }

    public int getUpdateDeferredSize() {
        return updatePool.size();
    }

    public int getDeleteDeferredSize() {
        return deletePool.size();
    }
}
