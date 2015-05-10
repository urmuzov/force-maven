package com.sforce.contrib.connection;

import com.sforce.contrib.partner.*;
import com.sforce.contrib.partner.Package;
import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sun.jersey.api.client.WebResource;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 20:44
 */
public interface Connection {
    int DEFAULT_BATCH_SIZE = 200;
    int SLOW_DOWN_OBJECT_COUNT = 5 * 200;
    double METADATA_API_VERSION = 32.0;

    MetadataConnection getMetadataConnection() throws ConnectionException;

    Map<ApiRequestType, Integer> getRequestCount();

    Map<ApiRequestType, Integer> getRequestCountAndClear();

    ConnectionDto getConnectionDto();

    Context getContext();

    PartnerConnection getPartnerConnection();

    String getSessionId();

    String getSalesforceDomainName();

    String getSalesforceInstance();

    String getApexRestUrl(com.sforce.contrib.partner.Package pkg);

    WebResource.Builder getApexRestUrlBuilder(Package pkg, String urlSuffix);

    String getDataRestUrl();

    WebResource.Builder getDateRestUrlBuilder(String urlSuffix);

    Transaction getTransaction();

    Transaction createTransaction();

    void create(SObject object) throws ConnectionException;

    void create(Collection<SObject> objects) throws ConnectionException;

    List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException;

    List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown, int batchSize) throws ConnectionException;

    void upsert(Field externalIdField, SObject object) throws ConnectionException;

    void upsert(Field externalIdField, Collection<SObject> objects) throws ConnectionException;

    List<Upsert> upsert(Field externalIdField, Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException;

    void update(SObject object) throws ConnectionException;

    void update(Collection<SObject> objects) throws ConnectionException;

    List<Save> update(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException;

    List<Delete> delete(SObject object, boolean emptyRecycleBin) throws ConnectionException;

    List<Delete> delete(Collection<SObject> objects, boolean emptyRecycleBin) throws ConnectionException;

    List<Delete> delete(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown, boolean emptyRecycleBin) throws ConnectionException;

    void flush(SObjectType objectMeta) throws ConnectionException;

    List<Delete> flush(SObjectType objectMeta, boolean exceptionOnFail) throws ConnectionException;

    void flush(String objectType) throws ConnectionException;

    List<Delete> flush(String objectType, boolean exceptionOnFail) throws ConnectionException;

    List<Delete> deleteById(String id, boolean emptyRecycleBin) throws ConnectionException;

    List<Delete> deleteById(Collection<String> ids, boolean emptyRecycleBin) throws ConnectionException;

    List<Delete> deleteById(Collection<String> ids, boolean exceptionOnFail, boolean slowDown, boolean emptyRecycleBin) throws ConnectionException;

    Result queryStart(Soql query) throws ConnectionException;

    Result queryMore(Result previousResult) throws ConnectionException;

    List<SObject> query(Soql query) throws ConnectionException;

    SObject querySingle(Soql query) throws ConnectionException;

    Integer getApiRequestLimit();

    Integer getApiRequestCount();

    <T extends Metadata> List<T> retrieveMetadata(Class<T> metadataClass) throws ConnectionException, IOException;

    <T extends Metadata> List<T> retrieveMetadata(String type, List<String> fullNames) throws ConnectionException;

    <T extends Metadata> List<SaveResult> createMetadata(List<T> objects) throws ConnectionException;

    <T extends Metadata> List<SaveResult> updateMetadata(List<T> objects) throws ConnectionException;

    <T extends Metadata> List<DeleteResult> deleteMetadata(List<T> objects, Class<T> clazz) throws ConnectionException;

    String retrievePackageVersion(Package pkg) throws ConnectionException, IOException;

    public static class Result {

        public final boolean done;
        public final String queryLocator;
        public final List<SObject> records;
        public final SObjectType type;
        public final String typeString;

        public Result(boolean done, String queryLocator, List<SObject> records, SObjectType type, String typeString) {
            this.done = done;
            this.queryLocator = queryLocator;
            this.records = records;
            this.type = type;
            this.typeString = typeString;
        }
    }
}
