package com.sforce.contrib.connection;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.SObject;
import com.sforce.contrib.partner.SObjectType;
import com.sforce.soap.metadata.*;
import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: urmuzov
 * Date: 12/4/12
 * Time: 7:04 AM
 */
public class Connection {

    public static final int DEFAULT_BATCH_SIZE = 200;
    public static final int SLOW_DOWN_OBJECT_COUNT = 5 * 200;

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

    public static String cropName(String name) {
        if (name == null) {
            return null;
        }
        if (name.length() <= 80) {
            return name;
        } else {
            return name.substring(0, 77) + "...";
        }
    }

    public static void slowDown() {
        try {
            logger.debug("Slowing down");
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean check(String username, String password, String token) {
        try {
            Connection.createNewConnection(username, password, token, new Context());
            return true;
        } catch (Exception ex) {
            return false;
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

    //private final Pattern SERVICE_ENDPOINT_PATTERN = Pattern.compile("^https://([^-]+)(-api)?.salesforce.com/.*$");
    // For example:
    // https://na15.salesforce.com/services/Soap/u/26.0/00Di0000000aNfm
    // https://cloudaware-7261.cloudforce.com/services/Soap/u/26.0/00Di0000000aNfm
    // https://c.na15.visual.force.com/services/Soap/u/28.0/00Di0000000JC88
    private final Pattern SERVICE_ENDPOINT_PATTERN = Pattern.compile("^https://(c\\.)?(.+)(-api)?\\.(salesforce|visual\\.force|cloudforce)\\.com/.*$");

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private final Client client = Client.create();

    private MetadataConnection metadataConnection;

    private ConnectionDto connectionDto;
    private final PartnerConnection connection;
    private Transaction transaction = new Transaction(this);
    private EnumMap<ApiRequestType, Integer> requestCount = Maps.newEnumMap(ApiRequestType.class);

    public static Connection createNewConnection(String username, String password, String token, Context context) throws ConnectionException {
        Connection connection = new Connection(createConnection(username, password, token), context);
        connection.onApiRequest(ApiRequestType.LOGIN);
        return connection;
    }

    public static Connection createNewConnection(String serviceEndpoint, String sessionId, Context context) throws ConnectionException {
        return new Connection(serviceEndpoint, sessionId, context);
    }

    public static Connection restoreConnection(ConnectionDto connectionDto) throws ConnectionException {
        return new Connection(connectionDto);
    }

    protected Connection(String serviceEndpoint, String sessionId, Context context) throws ConnectionException {
        this.connection = createConnection(serviceEndpoint, sessionId);
        this.connectionDto = createConnectionDto(this.connection, context);
    }

    protected Connection(PartnerConnection partnerConnection, Context context) throws ConnectionException {
        this.connection = partnerConnection;
        this.connectionDto = createConnectionDto(partnerConnection, context);
    }

    protected Connection(ConnectionDto connectionDto) throws ConnectionException {
        this.connection = createConnection(connectionDto.getServiceEndpoint(), connectionDto.getSessionId());
        this.connectionDto = connectionDto;
    }

    private static ConnectionDto createConnectionDto(PartnerConnection partnerConnection, Context context) throws ConnectionException {
        GetUserInfoResult userInfo = partnerConnection.getUserInfo();
        ConnectorConfig config = partnerConnection.getConfig();
        ConnectionDto connectionDto = new ConnectionDto(
                config.getSessionId(),
                config.getServiceEndpoint(),
                userInfo.getOrganizationId(),
                userInfo.getOrganizationName(),
                userInfo.getProfileId(),
                userInfo.getRoleId(),
                userInfo.getSessionSecondsValid(),
                userInfo.getUserEmail(),
                userInfo.getUserFullName(),
                userInfo.getUserId(),
                userInfo.getUserName(),
                context == null ? new Context() : context
        );
        logger.info("Connection created: {}.", connectionDto);

        return connectionDto;
    }

    private static PartnerConnection createConnection(String username, String password, String token) throws ConnectionException {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password");
        }
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token");
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setUsername(username);
        config.setPassword(password + token);
        config.setAuthEndpoint("https://login.salesforce.com/services/Soap/u/29.0");

        return new PartnerConnection(config);
    }

    private static PartnerConnection createConnection(String serviceEndpoint, String sessionId) throws ConnectionException {
        if (serviceEndpoint == null || serviceEndpoint.isEmpty()) {
            throw new IllegalArgumentException("serviceEndpoint");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("sessionId");
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setSessionId(sessionId);
        config.setServiceEndpoint(serviceEndpoint);
        config.setConnectionTimeout(600000);
        config.setReadTimeout(600000);

        return new PartnerConnection(config);
    }

    public MetadataConnection getMetadataConnection() throws ConnectionException {
        if (metadataConnection == null) {
            ConnectorConfig metadataConfig = new ConnectorConfig();
            ConnectorConfig config = this.connection.getConfig();
            // Строки подключения к data api:
            // https://na15.salesforce.com/services/Soap/u/26.0/00Di0000000JC88
            // https://c.na15.visual.force.com/services/Soap/u/28.0/00Di0000000JC88
            // Соответственно строки для metadata api:
            // https://na15.salesforce.com/services/Soap/m/26.0/00Di0000000JC88
            // https://c.na15.visual.force.com/services/Soap/m/28.0/00Di0000000JC88
            // Заменяем вхождение "/u/" на "/m/":
            metadataConfig.setServiceEndpoint(config.getServiceEndpoint().replaceFirst("/u/", "/m/"));
            metadataConfig.setSessionId(config.getSessionId());
            metadataConfig.setConnectionTimeout(config.getConnectionTimeout());
            metadataConfig.setReadTimeout(config.getReadTimeout());
            metadataConnection = new MetadataConnection(metadataConfig);
        }

        return metadataConnection;
    }

    protected void onApiRequest(ApiRequestType apiRequest) {
        if (requestCount.containsKey(apiRequest)) {
            requestCount.put(apiRequest, requestCount.get(apiRequest) + 1);
        } else {
            requestCount.put(apiRequest, 1);
        }
    }

    public Map<ApiRequestType, Integer> getRequestCount() {
        return ImmutableMap.copyOf(requestCount);
    }

    public synchronized Map<ApiRequestType, Integer> getRequestCountAndClear() {
        ImmutableMap<ApiRequestType, Integer> out = ImmutableMap.copyOf(requestCount);
        requestCount.clear();
        return out;
    }

    public ConnectionDto getConnectionDto() {
        return connectionDto;
    }

    public Context getContext() {
        return connectionDto.getContext();
    }

    public PartnerConnection getPartnerConnection() {
        return connection;
    }

    public String getSessionId() {
        String sessionId = connection.getConfig().getSessionId();
        return sessionId.substring(sessionId.indexOf("!") + 1);
    }

    public String getSalesforceDomainName() {
        String serviceEndpoint = connection.getConfig().getServiceEndpoint();
        Matcher matcher = SERVICE_ENDPOINT_PATTERN.matcher(serviceEndpoint);
        if (!matcher.matches()) {
            throw new RuntimeException("Can't find salesforce domain name in serviceEndpoint url");
        }

        String name = matcher.group(4);
        if (name.equalsIgnoreCase("visual.force")) {
            name = "salesforce";
        }

        return name;
    }

    public String getSalesforceInstance() {
        String serviceEndpoint = connection.getConfig().getServiceEndpoint();
        Matcher matcher = SERVICE_ENDPOINT_PATTERN.matcher(serviceEndpoint);
        if (!matcher.matches()) {
            throw new RuntimeException("Can't find salesforce instance id in serviceEndpoint url");
        }
        return matcher.group(2);
    }

    public String getApexRestUrl(Package pkg) {
        String namespace = getContext().get(pkg);
        return "https://" + getSalesforceInstance() + "." + getSalesforceDomainName() + ".com/services/apexrest" + (Strings.isNullOrEmpty(namespace) ? "" : "/" + namespace);
    }

    public WebResource.Builder getApexRestUrlBuilder(Package pkg, String urlSuffix) {
        String url = getApexRestUrl(pkg) + urlSuffix;
        logger.info("APEX REST Request: {}", url);
        onApiRequest(ApiRequestType.APEX_REST);
        return client.resource(url)
                .header("Authorization", "Bearer " + getSessionId());
    }

    public String getDataRestUrl() {
        return "https://" + getSalesforceInstance() + "." + getSalesforceDomainName() + ".com/services/data/v29.0";
    }

    public WebResource.Builder getDateRestUrlBuilder(String urlSuffix) {
        String url = getDataRestUrl() + urlSuffix;
        logger.info("Date REST Request: {}", url);
        onApiRequest(ApiRequestType.DATA);
        return client.resource(url)
                .header("Authorization", "Bearer " + getSessionId());
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Transaction createTransaction() {
        return new Transaction(this);
    }

    public void create(SObject object) throws ConnectionException {
        create(Lists.newArrayList(object));
    }

    public void create(Collection<SObject> objects) throws ConnectionException {
        create(objects, true, false);
    }

    public List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        return create(objects, exceptionOnFail, slowDown, DEFAULT_BATCH_SIZE);
    }

    public List<Save> create(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown, int batchSize) throws ConnectionException {
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }

        logger.info("Connection will create {} objects; exceptionOnFail: {}; slowDown: {}; batchSize: {}", objects.size(), exceptionOnFail, slowDown, batchSize);
        List<List<SObject>> data = makeBatch(objects, batchSize);
        int processed = 0;
        int failsCount = 0;
        List<Save> out = Lists.newArrayList();
        for (List<SObject> batch : data) {
            logger.info("Creating {} objects. {} left", objects.size(), objects.size() - processed);
            com.sforce.soap.partner.sobject.SObject[] converted = convertSObjects(batch, getContext());
            com.sforce.soap.partner.SaveResult[] result = connection.create(converted);
            onApiRequest(ApiRequestType.CREATE);
            for (int i = 0; i < result.length; i++) {
                com.sforce.soap.partner.SaveResult r = result[i];
                Save save = new Save(batch.get(i), r, converted[i]);
                out.add(save);
                if (!r.isSuccess()) {
                    if (exceptionOnFail) {
                        throw save;
                    } else {
                        failsCount++;
                    }
                } else {
                    batch.get(i).setId(r.getId());
                }
            }
            processed += batch.size();
            if (slowDown) {
                slowDown();
            }
        }

        if (failsCount > 0) {
            logger.warn("Connection create objects failed. Errors: {}", failsCount);
        } else {
            logger.info("Connection objects created successfully.");
        }

        return out;
    }

    public void upsert(Field externalIdField, SObject object) throws ConnectionException {
        upsert(externalIdField, ImmutableList.of(object), true, false);
    }

    public void upsert(Field externalIdField, Collection<SObject> objects) throws ConnectionException {
        upsert(externalIdField, objects, true, false);
    }

    public List<Upsert> upsert(Field externalIdField, Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }

        logger.info("Connection will upsert {} objects; exceptionOnFail: {}; slowDown: {}", objects.size(), exceptionOnFail, slowDown);
        List<List<SObject>> data = makeBatch(objects);
        int processed = 0;
        int failsCount = 0;
        List<Upsert> out = Lists.newArrayList();
        for (List<SObject> batch : data) {
            logger.info("Upserting {} objects. {} left", objects.size(), objects.size() - processed);
            com.sforce.soap.partner.sobject.SObject[] converted = convertSObjects(batch, getContext());
            com.sforce.soap.partner.UpsertResult[] result = connection.upsert(externalIdField.apiName(getContext()), converted);
            onApiRequest(ApiRequestType.UPSERT);
            for (int i = 0; i < result.length; i++) {
                com.sforce.soap.partner.UpsertResult r = result[i];
                out.add(new Upsert(batch.get(i), r, converted[i]));
                if (!r.isSuccess()) {
                    if (exceptionOnFail) {
                        throw new RuntimeException(r.toString());
                    } else {
                        failsCount++;
                    }
                }
            }
            processed += batch.size();
            if (slowDown) {
                slowDown();
            }
        }

        if (failsCount > 0) {
            logger.warn("Connection upsert objects failed. Errors: {}", failsCount);
        } else {
            logger.info("Connection objects upserted successfully.");
        }

        return out;
    }

    public void update(SObject object) throws ConnectionException {
        update(connection, ImmutableList.of(object), true, false);
    }

    public void update(Collection<SObject> objects) throws ConnectionException {
        update(connection, objects, true, false);
    }

    public List<Save> update(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        return update(connection, objects, exceptionOnFail, slowDown);
    }

    public List<Save> update(PartnerConnection connection, Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }

        logger.info("Connection will update {} objects; exceptionOnFail: {}; slowDown: {}", objects.size(), exceptionOnFail, slowDown);
        List<List<SObject>> data = makeBatch(objects);
        int processed = 0;
        int failsCount = 0;
        List<Save> out = Lists.newArrayList();
        for (List<SObject> batch : data) {
            logger.info("Updating {} objects. {} left", objects.size(), objects.size() - processed);
            com.sforce.soap.partner.sobject.SObject[] converted = convertSObjects(batch, getContext());
            com.sforce.soap.partner.SaveResult[] result = connection.update(converted);
            onApiRequest(ApiRequestType.UPDATE);
            for (int i = 0; i < result.length; i++) {
                com.sforce.soap.partner.SaveResult r = result[i];
                out.add(new Save(batch.get(i), r, converted[i]));
                if (!r.isSuccess()) {
                    if (exceptionOnFail) {
                        throw new RuntimeException(r.toString());
                    } else {
                        failsCount++;
                    }
                }
            }
            processed += batch.size();
            if (slowDown) {
                slowDown();
            }
        }

        if (failsCount > 0) {
            logger.warn("Connection update objects failed. Errors: {}", failsCount);
        } else {
            logger.info("Connection objects updated successfully.");
        }

        return out;
    }

    public List<Delete> delete(SObject object) throws ConnectionException {
        return delete(ImmutableList.of(object), true, false);
    }

    public List<Delete> delete(Collection<SObject> objects) throws ConnectionException {
        return delete(objects, true, false);
    }

    public List<Delete> delete(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        if (objects.isEmpty()) {
            return ImmutableList.of();
        }
        List<String> ids = Lists.newArrayList();
        for (SObject o : objects) {
            ids.add(o.getId());
        }
        return deleteById(ids, exceptionOnFail, slowDown);
    }

    public void flush(SObjectType objectMeta) throws ConnectionException {
        flush(objectMeta.apiName(getContext()), true);
    }

    public List<Delete> flush(SObjectType objectMeta, boolean exceptionOnFail) throws ConnectionException {
        return flush(objectMeta.apiName(getContext()), exceptionOnFail);
    }

    public void flush(String objectType) throws ConnectionException {
        flush(objectType, true);
    }

    public List<Delete> flush(String objectType, boolean exceptionOnFail) throws ConnectionException {
        List<SObject> result = query(Soql.from(objectType).select("Id"));
        List<String> ids = Lists.newArrayList();
        for (SObject o : result) {
            ids.add(o.getId());
        }
        return deleteById(ids, exceptionOnFail, false);
    }

    public List<Delete> deleteById(String id) throws ConnectionException {
        return deleteById(ImmutableList.of(id));
    }

    public List<Delete> deleteById(Collection<String> ids) throws ConnectionException {
        return deleteById(ids, true, false);
    }

    public List<Delete> deleteById(Collection<String> ids, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
        if (ids.isEmpty()) {
            return ImmutableList.of();
        }

        logger.info("Connection will delete {} objects; exceptionOnFail: {}; slowDown: {}", ids.size(), exceptionOnFail, slowDown);
        List<String> idsList = Lists.newArrayList(ids);
        int failsCount = 0;
        List<Delete> out = Lists.newArrayList();
        while (!idsList.isEmpty()) {
            logger.info("Deleting {} objects. {} left", ids.size(), idsList.size());
            List<String> portion = idsList.subList(0, Math.min(DEFAULT_BATCH_SIZE, idsList.size()));
            com.sforce.soap.partner.DeleteResult[] res = connection.delete(portion.toArray(new String[0]));
            onApiRequest(ApiRequestType.DELETE);
            for (int i = 0; i < res.length; i++) {
                com.sforce.soap.partner.DeleteResult r = res[i];
                Delete d = new Delete(portion.get(i), r);
                out.add(d);
                if (!r.isSuccess()) {
                    if (exceptionOnFail) {
                        throw d;
                    } else {
                        failsCount++;
                    }
                }
            }
            portion.clear();
            if (slowDown) {
                slowDown();
            }
        }

        if (failsCount > 0) {
            logger.warn("Connection delete objects failed. Errors: {}", failsCount);
        } else {
            logger.info("Connection objects deleted successfully.");
        }

        return out;
    }

    public List<SObject> query(Soql query) throws ConnectionException {
        String q = query.compile(getContext());
        QueryResult result = connection.query(q);
        onApiRequest(ApiRequestType.QUERY);
        logger.info("Quering {} '{}'", result.getQueryLocator(), q);
        List<SObject> out = Lists.newArrayList();
        while (true) {
            for (com.sforce.soap.partner.sobject.SObject oIn : result.getRecords()) {
                if (query.getFromType() == null) {
                    out.add(new SObject(oIn));
                } else {
                    out.add(new SObject(oIn, query.getFromType()));
                }
            }
            if (!result.isDone()) {
                result = connection.queryMore(result.getQueryLocator());
                onApiRequest(ApiRequestType.QUERY);
                logger.info("Quering more {}", result.getQueryLocator());
            } else {
                break;
            }
        }
        return out;
    }

    public SObject querySingle(Soql query) throws ConnectionException {
        List<SObject> result = query(query);
        return result.isEmpty() ? null : result.get(0);
    }

    private LimitInfo getApiRequestLimitInfo() {
        if (connection.getLimitInfoHeader() == null) {
            return null;
        }
        if (connection.getLimitInfoHeader().getLimitInfo() == null) {
            return null;
        }
        for (LimitInfo limit : connection.getLimitInfoHeader().getLimitInfo()) {
            if (limit.getType().equals("API REQUESTS")) {
                return limit;
            }
        }
        return null;
    }

    public Integer getApiRequestLimit() {
        LimitInfo info = getApiRequestLimitInfo();
        return info == null ? null : info.getLimit();
    }

    public Integer getApiRequestCount() {
        LimitInfo info = getApiRequestLimitInfo();
        return info == null ? null : info.getCurrent();
    }


    /**
     * METADATA
     */
    private static final int METADATA_BATCH_SIZE = 10;
    public static final double METADATA_API_VERSION = 32.0;

    public <T extends Metadata> List<T> retrieveMetadata(Class<T> metadataClass) throws ConnectionException, IOException {
        String type = metadataClass.getSimpleName();
        List<ListMetadataQuery> queries = Lists.newArrayList();
        if (!isFolderSpecific(metadataClass)) {
            ListMetadataQuery query = new ListMetadataQuery();
            query.setType(type);
            queries.add(query);
        } else {
            ListMetadataQuery folderQuery = new ListMetadataQuery();
            folderQuery.setType(type + "Folder");
            List<String> folders = Lists.newArrayList();
            for (FileProperties prop : getMetadataConnection().listMetadata(new ListMetadataQuery[]{ folderQuery }, METADATA_API_VERSION)) {
                folders.add(prop.getFullName());
            }

            for (String folder : folders) {
                ListMetadataQuery query = new ListMetadataQuery();
                query.setType(type);
                query.setFolder(folder);
                queries.add(query);
            }
        }

        List<String> fullNames = Lists.newArrayList();
        FileProperties[] fileProperties = getMetadataConnection().listMetadata(queries.toArray(new ListMetadataQuery[queries.size()]), METADATA_API_VERSION);
        for (FileProperties file : fileProperties) {
            fullNames.add(file.getFullName());
        }

        return retrieveMetadata(type, fullNames);
    }

    public <T extends Metadata> List<T> retrieveMetadata(String type, List<String> fullNames) throws ConnectionException {
        List<Metadata> objects = Lists.newArrayList();
        List<String> batch = Lists.newArrayList();
        for (int i = 0; i < fullNames.size(); i++) {
            batch.add(fullNames.get(i));
            if ((i + 1) % METADATA_BATCH_SIZE == 0) {
                objects.addAll(Lists.newArrayList(getMetadataConnection().readMetadata(type, batch.toArray(new String[batch.size()])).getRecords()));
                batch.clear();
            }
        }

        if (batch.size() != 0) {
            objects.addAll(Lists.newArrayList(getMetadataConnection().readMetadata(type, batch.toArray(new String[batch.size()])).getRecords()));
        }

        List<T> out = Lists.newArrayList();
        for (Metadata entry : objects) {
            out.add((T)entry);
        }

        return out;
    }

    public <T extends Metadata> List<SaveResult> createMetadata(List<T> objects) throws ConnectionException {
        List<SaveResult> out = Lists.newArrayList();
        List<Metadata> batch = Lists.newArrayList();
        MetadataConnection c = getMetadataConnection();
        for (int i = 0; i < objects.size(); i++) {
            batch.add(objects.get(i));
            if ((i + 1) % METADATA_BATCH_SIZE == 0) {
                out.addAll(Lists.newArrayList(c.createMetadata(batch.toArray(new Metadata[batch.size()]))));
                batch.clear();
            }
        }

        if (batch.size() != 0) {
            out.addAll(Lists.newArrayList(c.createMetadata(batch.toArray(new Metadata[batch.size()]))));
            batch.clear();
        }

        return out;
    }

    public <T extends Metadata> List<SaveResult> updateMetadata(List<T> objects) throws ConnectionException {
        List<SaveResult> out = Lists.newArrayList();
        List<Metadata> batch = Lists.newArrayList();
        MetadataConnection c = getMetadataConnection();
        for (int i = 0; i < objects.size(); i++) {
            batch.add(objects.get(i));
            if ((i + 1) % METADATA_BATCH_SIZE == 0) {
                out.addAll(Lists.newArrayList(c.updateMetadata(batch.toArray(new Metadata[batch.size()]))));
                batch.clear();
            }
        }

        if (batch.size() != 0) {
            out.addAll(Lists.newArrayList(c.updateMetadata(batch.toArray(new Metadata[batch.size()]))));
            batch.clear();
        }

        return out;
    }

    public <T extends Metadata> List<DeleteResult> deleteMetadata(List<T> objects, Class<T> clazz) throws ConnectionException {
        List<DeleteResult> out = Lists.newArrayList();
        List<Metadata> batch = Lists.newArrayList();
        MetadataConnection c = getMetadataConnection();

        String type = clazz.getSimpleName();
        List<String> fullNamesList = Lists.newArrayList();
        for (T object : objects) {
            fullNamesList.add(object.getFullName());
        }

        String[] fullNames = fullNamesList.toArray(new String[fullNamesList.size()]);
        for (int i = 0; i < objects.size(); i++) {
            batch.add(objects.get(i));
            if ((i + 1) % METADATA_BATCH_SIZE == 0) {
                out.addAll(Lists.newArrayList(c.deleteMetadata(type, fullNames)));
                batch.clear();
            }
        }

        if (batch.size() != 0) {
            out.addAll(Lists.newArrayList(c.deleteMetadata(type, fullNames)));
            batch.clear();
        }

        return out;
    }

    public String retrievePackageVersion(Package pkg) throws ConnectionException, IOException {
        String namespace = getContext().get(pkg);
        if (Strings.isNullOrEmpty(namespace)) {
            return null;
        }

        List<InstalledPackage> installedPackages = retrieveMetadata(InstalledPackage.class);
        for (InstalledPackage installedPackage : installedPackages) {
            if (namespace.equals(installedPackage.getFullName())) {
                return installedPackage.getVersionNumber();
            }
        }

        return null;
    }

    private boolean isFolderSpecific(Class metadataClass) {
        return metadataClass == Document.class
                || metadataClass == Email.class
                || metadataClass == Report.class
                || metadataClass == Dashboard.class;
    }
}