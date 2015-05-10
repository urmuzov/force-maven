package com.sforce.contrib.connection;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sforce.contrib.partner.*;
import com.sforce.contrib.partner.Package;
import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.ws.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 21:39
 */
public class ConnectionMock extends ConnectionBase implements Connection {

    private static class Res extends Result {
        private final String query;
        private final int batchNumber;

        public Res(String query, int batchNumber, boolean done, String queryLocator, List<SObject> records, SObjectType type, String typeString) {
            super(done, queryLocator, records, type, typeString);
            this.query = query;
            this.batchNumber = batchNumber;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMock.class);

    private final Context context;
    private List<SObject> created = Lists.newArrayList();
    private List<SObject> upserted = Lists.newArrayList();
    private List<SObject> updated = Lists.newArrayList();
    private List<String> deleted = Lists.newArrayList();
    private Map<String, List<SObject>> results = Maps.newLinkedHashMap();

    public ConnectionMock(Context context) {
        this.context = context;
    }

    public void addTestQueryResult(Soql query, List<SObject> result) {
        String q = query.compile(getContext());
        results.put(q, result);
    }

    public List<SObject> getCreated() {
        return created;
    }

    public List<SObject> getUpserted() {
        return upserted;
    }

    public List<SObject> getUpdated() {
        return updated;
    }

    public List<String> getDeleted() {
        return deleted;
    }

    @Override
    public MetadataConnection getMetadataConnection() throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDto getConnectionDto() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public PartnerConnection getPartnerConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSalesforceDomainName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSalesforceInstance() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown, int batchSize) throws ConnectionException {
        created.addAll(objects);
        List<Save> out = Lists.newArrayList();
        for (SObject object : objects) {
            out.add(new Save(object, new com.sforce.soap.partner.SaveResult(), null));
            object.setId(UUID.randomUUID().toString());
        }
        return out;
    }

    @Override
    public List<Upsert> upsert(Field externalIdField, Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        upserted.addAll(objects);
        List<Upsert> out = Lists.newArrayList();
        for (SObject object : objects) {
            out.add(new Upsert(object, new UpsertResult(), null));
            if (object.getId() == null) {
                object.setId(UUID.randomUUID().toString());
            }
        }
        return out;
    }

    @Override
    public List<Save> update(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        updated.addAll(objects);
        List<Save> out = Lists.newArrayList();
        for (SObject object : objects) {
            out.add(new Save(object, new com.sforce.soap.partner.SaveResult(), null));
        }
        return out;
    }

    @Override
    public List<Delete> deleteById(Collection<String> ids, boolean exceptionOnFail, boolean slowDown, boolean emptyRecycleBin) throws ConnectionException {
        deleted.addAll(ids);
        List<Delete> out = Lists.newArrayList();
        for (String id : ids) {
            out.add(new Delete(id, new com.sforce.soap.partner.DeleteResult()));
        }
        return out;
    }

    @Override
    public List<Delete> flush(String objectType, boolean exceptionOnFail) throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result queryStart(Soql query) throws ConnectionException {
        String q = query.compile(getContext());
        List<SObject> res = results.get(q);
        if (res == null) {
            logger.info("QueryStart: [no-data] {}", q);
            return new Result(true, null, Lists.<SObject>newArrayList(), query.getFromType(), query.getFromString());
        } else {
            logger.info("QueryStart: [batch 0] {}", q);
            return new Res(q, 0, res.size() <= 2000, UUID.randomUUID().toString(), res.subList(0, Math.min(res.size(), 2000)), query.getFromType(), query.getFromString());
        }
    }

    @Override
    public Result queryMore(Result previousResult) throws ConnectionException {
        Res previousRes = (Res) previousResult;
        int thisBatch = previousRes.batchNumber + 1;
        List<SObject> res = results.get(previousRes.query);
        logger.info("QueryStart: [batch {}] {}", thisBatch, previousRes.query);
        return new Res(
                previousRes.query,
                thisBatch,
                res.size() <= (thisBatch + 1) * 2000,
                UUID.randomUUID().toString(),
                res.subList(thisBatch * 2000, Math.min(res.size(), (thisBatch + 1) * 2000)),
                previousRes.type,
                previousRes.typeString
        );
    }

    @Override
    public Integer getApiRequestLimit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getApiRequestCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Metadata> List<T> retrieveMetadata(Class<T> metadataClass) throws ConnectionException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Metadata> List<T> retrieveMetadata(String type, List<String> fullNames) throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Metadata> List<SaveResult> createMetadata(List<T> objects) throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Metadata> List<SaveResult> updateMetadata(List<T> objects) throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Metadata> List<DeleteResult> deleteMetadata(List<T> objects, Class<T> clazz) throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String retrievePackageVersion(Package pkg) throws ConnectionException, IOException {
        throw new UnsupportedOperationException();
    }
}
