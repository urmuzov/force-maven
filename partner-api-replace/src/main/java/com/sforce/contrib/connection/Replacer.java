package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.sforce.contrib.partner.SObject;
import com.sforce.ws.ConnectionException;

import java.util.LinkedList;
import java.util.List;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 19:55
 */
public class Replacer {
    private final Connection connection;
    private final Soql query;
    private ConnectionImpl.Result currentBatch;
    private LinkedList<String> availableIds;
    private boolean moreIdsAvailable;
    private List<SObject> updateBatch = Lists.newArrayList();
    private List<SObject> insertBatch = Lists.newArrayList();

    public Replacer(Connection connection, Soql query) throws ConnectionException {
        this.connection = connection;
        this.query = query;
        currentBatch = connection.queryStart(query);
        availableIds = toIds(currentBatch.records);
        moreIdsAvailable = !currentBatch.done;
    }

    private static LinkedList<String> toIds(List<SObject> records) {
        LinkedList<String> out = Lists.newLinkedList();
        for (SObject record : records) {
            out.add(record.getId());
        }
        return out;
    }

    private void queryMoreIfNeeded() throws ConnectionException {
        if (availableIds.isEmpty() && moreIdsAvailable) {
            currentBatch = connection.queryMore(currentBatch);
            availableIds = toIds(currentBatch.records);
            moreIdsAvailable = !currentBatch.done;
        }
    }

    public void updateOrInsert(SObject sObject) throws ConnectionException {
        queryMoreIfNeeded();
        if (!availableIds.isEmpty()) {
            String id = availableIds.remove();
            sObject.setId(id);
            updateBatch.add(sObject);
            if (updateBatch.size() >= Connection.DEFAULT_BATCH_SIZE) {
                connection.update(updateBatch);
                updateBatch.clear();
            }
        } else {
            insertBatch.add(sObject);
            if (insertBatch.size() >= Connection.DEFAULT_BATCH_SIZE) {
                connection.create(insertBatch);
                insertBatch.clear();
            }
        }
    }

    public void deleteUnused() throws ConnectionException {
        if (!updateBatch.isEmpty()) {
            connection.update(updateBatch);
        }
        if (!insertBatch.isEmpty()) {
            connection.create(insertBatch);
        }
        if (!availableIds.isEmpty()) {
            connection.deleteById(availableIds, true);
        }
        if (moreIdsAvailable) {
            while (true) {
                currentBatch = connection.queryMore(currentBatch);
                connection.delete(currentBatch.records, true);
                if (currentBatch.done) {
                    break;
                }
            }
        }
    }
}
