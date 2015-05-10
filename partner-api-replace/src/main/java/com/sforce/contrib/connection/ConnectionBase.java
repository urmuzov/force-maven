package com.sforce.contrib.connection;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.sforce.contrib.partner.*;
import com.sforce.contrib.partner.Package;
import com.sforce.ws.ConnectionException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: urmuzov
 * Date: 15-05-08
 * Time: 22:13
 */
public abstract class ConnectionBase implements Connection {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionBase.class);

    public static List<List<SObject>> makeBatch(Collection<SObject> collection) {
        return makeBatch(collection, DEFAULT_BATCH_SIZE);
    }

    public static List<List<SObject>> makeBatch(Collection<SObject> collection, int batchSize) {
        Multimap<String, SObject> byType = TreeMultimap.create(new Comparator<String>() {
                                                                   @Override
                                                                   public int compare(String o1, String o2) {
                                                                       return o1 == null ? (o2 == null ? 0 : 1) : o1.compareTo(o2);
                                                                   }
                                                               }, new Comparator<SObject>() {
                                                                   @Override
                                                                   public int compare(SObject o1, SObject o2) {
                                                                       return -1;
                                                                   }
                                                               }
        );
        for (SObject object : collection) {
            if (object == null) {
                throw new IllegalArgumentException("Expected SObject found null");
            }
            byType.put(object.getType() == null ? object.getTypeString() : object.getType().sfName(), object);
        }
        List<List<SObject>> out = Lists.newArrayList();
        List<SObject> batch = Lists.newArrayList();
        String lastType = null;
        int typesInBatch = 0;
        for (Map.Entry<String, SObject> e : byType.entries()) {
            if (!e.getKey().equals(lastType)) {
                lastType = e.getKey();
                typesInBatch += 1;
            }
            if (typesInBatch > 10 || batch.size() >= batchSize) {
                out.add(batch);
                batch = Lists.newArrayList();
                typesInBatch = 1;
            }
            batch.add(e.getValue());
        }
        out.add(batch);
        return out;
    }

    public static com.sforce.soap.partner.sobject.SObject[] convertSObjects(List<SObject> sObjects, Context context) {
        com.sforce.soap.partner.sobject.SObject[] out = new com.sforce.soap.partner.sobject.SObject[sObjects.size()];
        for (int i = 0; i < sObjects.size(); i++) {
            out[i] = sObjects.get(i).convert(context);
        }
        return out;
    }

    public static void slowDown() {
        try {
            logger.debug("Slowing down");
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static String compileQuery(String queryString, List parameters) {
        Pattern patt = Pattern.compile("\\?");
        Matcher m = patt.matcher(queryString);
        StringBuffer sb = new StringBuffer(queryString.length());
        int i = 0;
        Object param;
        while (m.find()) {
            try {
                param = parameters.get(i);
                if (param == null) {
                    m.appendReplacement(sb, "null");
                } else if (param instanceof Iterator) {
                    m.appendReplacement(sb, Joiner.on(", ").join((Iterator) param));
                } else if (param instanceof Iterable) {
                    m.appendReplacement(sb, Joiner.on(", ").join((Iterable) param));
                } else {
                    m.appendReplacement(sb, param.toString());
                }
                i++;
            } catch (IndexOutOfBoundsException ex) {
                throw new RuntimeException("Can't find parameter " + i + " for query '" + queryString + "' and parameters " + Arrays.asList(parameters), ex);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Transaction transaction = new Transaction(this);
    private final Client client = Client.create();
    private EnumMap<ApiRequestType, Integer> requestCount = Maps.newEnumMap(ApiRequestType.class);

    protected final void onApiRequest(ApiRequestType apiRequest) {
        if (requestCount.containsKey(apiRequest)) {
            requestCount.put(apiRequest, requestCount.get(apiRequest) + 1);
        } else {
            requestCount.put(apiRequest, 1);
        }
    }

    @Override
    public final Transaction getTransaction() {
        return transaction;
    }

    @Override
    public final Transaction createTransaction() {
        return new Transaction(this);
    }

    @Override
    public final void create(SObject object) throws ConnectionException {
        create(Lists.newArrayList(object));
    }

    @Override
    public final void create(Collection<SObject> objects) throws ConnectionException {
        create(objects, true, false);
    }

    @Override
    public final List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        return create(objects, exceptionOnFail, slowDown, DEFAULT_BATCH_SIZE);
    }

    @Override
    public final void upsert(Field externalIdField, SObject object) throws ConnectionException {
        upsert(externalIdField, ImmutableList.of(object), true, false);
    }

    @Override
    public final void upsert(Field externalIdField, Collection<SObject> objects) throws ConnectionException {
        upsert(externalIdField, objects, true, false);
    }

    @Override
    public final void update(SObject object) throws ConnectionException {
        update(ImmutableList.of(object), true, false);
    }

    @Override
    public final void update(Collection<SObject> objects) throws ConnectionException {
        update(objects, true, false);
    }

    @Override
    public final List<Delete> delete(SObject object, boolean emptyRecycleBin) throws ConnectionException {
        return delete(ImmutableList.of(object), true, false, emptyRecycleBin);
    }

    @Override
    public final List<Delete> delete(Collection<SObject> objects, boolean emptyRecycleBin) throws ConnectionException {
        return delete(objects, true, false, emptyRecycleBin);
    }

    @Override
    public final List<Delete> delete(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown, boolean emptyRecycleBin) throws ConnectionException {
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }
        List<String> ids = Lists.newArrayList();
        for (SObject o : objects) {
            ids.add(o.getId());
        }
        return deleteById(ids, exceptionOnFail, slowDown, emptyRecycleBin);
    }

    @Override
    public final List<Delete> deleteById(String id, boolean emptyRecycleBin) throws ConnectionException {
        return deleteById(ImmutableList.of(id), emptyRecycleBin);
    }

    @Override
    public final List<Delete> deleteById(Collection<String> ids, boolean emptyRecycleBin) throws ConnectionException {
        return deleteById(ids, true, false, emptyRecycleBin);
    }

    @Override
    public final void flush(SObjectType objectMeta) throws ConnectionException {
        flush(objectMeta.apiName(getContext()), true);
    }

    @Override
    public final List<Delete> flush(SObjectType objectMeta, boolean exceptionOnFail) throws ConnectionException {
        return flush(objectMeta.apiName(getContext()), exceptionOnFail);
    }

    @Override
    public final void flush(String objectType) throws ConnectionException {
        flush(objectType, true);
    }

    @Override
    public final List<SObject> query(Soql query) throws ConnectionException {
        Result result = queryStart(query);
        List<SObject> out = Lists.newArrayList();
        out.addAll(result.records);
        while (true) {
            if (!result.done) {
                result = queryMore(result);
                out.addAll(result.records);
            } else {
                break;
            }
        }
        return out;
    }

    @Override
    public final SObject querySingle(Soql query) throws ConnectionException {
        List<SObject> result = query(query);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public final String getApexRestUrl(com.sforce.contrib.partner.Package pkg) {
        String namespace = getContext().get(pkg);
        return "https://" + getSalesforceInstance() + "." + getSalesforceDomainName() + ".com/services/apexrest" + (Strings.isNullOrEmpty(namespace) ? "" : "/" + namespace);
    }

    @Override
    public final WebResource.Builder getApexRestUrlBuilder(Package pkg, String urlSuffix) {
        String url = getApexRestUrl(pkg) + urlSuffix;
        logger.info("APEX REST Request: {}", url);
        onApiRequest(ApiRequestType.APEX_REST);
        return client.resource(url)
                .header("Authorization", "Bearer " + getSessionId());
    }

    @Override
    public final String getDataRestUrl() {
        return "https://" + getSalesforceInstance() + "." + getSalesforceDomainName() + ".com/services/data/v29.0";
    }

    @Override
    public final WebResource.Builder getDateRestUrlBuilder(String urlSuffix) {
        String url = getDataRestUrl() + urlSuffix;
        logger.info("Date REST Request: {}", url);
        onApiRequest(ApiRequestType.DATA);
        return client.resource(url)
                .header("Authorization", "Bearer " + getSessionId());
    }

    @Override
    public final Map<ApiRequestType, Integer> getRequestCount() {
        return ImmutableMap.copyOf(requestCount);
    }

    @Override
    public final synchronized Map<ApiRequestType, Integer> getRequestCountAndClear() {
        ImmutableMap<ApiRequestType, Integer> out = ImmutableMap.copyOf(requestCount);
        requestCount.clear();
        return out;
    }
}
