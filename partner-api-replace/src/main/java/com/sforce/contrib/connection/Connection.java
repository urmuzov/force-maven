package com.sforce.contrib.connection;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.sforce.contrib.metadata.FixedTypeMapper;
import com.sforce.contrib.partner.*;
import com.sforce.contrib.partner.Field;
import com.sforce.contrib.partner.Package;
import com.sforce.soap.metadata.*;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.parser.PullParserException;
import com.sforce.ws.parser.XmlInputStream;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static Connection createNewConnection(String username, String password, String token, Context context) throws ConnectionException {
        return new Connection(createConnection(username, password, token), context);
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

        return new PartnerConnection(config);
    }

    protected MetadataConnection getMetadataConnection() throws ConnectionException {
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
            metadataConnection = new MetadataConnection(metadataConfig);
        }

        return metadataConnection;
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
        return client.resource(url)
                .header("Authorization", "Bearer " + getSessionId());
    }

    public String getDataRestUrl() {
        return "https://" + getSalesforceInstance() + "." + getSalesforceDomainName() + ".com/services/data/v29.0";
    }

    public WebResource.Builder getDateRestUrlBuilder(String urlSuffix) {
        String url = getDataRestUrl() + urlSuffix;
        logger.info("Date REST Request: {}", url);
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
            SaveResult[] result = connection.create(converted);
            for (int i = 0; i < result.length; i++) {
                SaveResult r = result[i];
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
            UpsertResult[] result = connection.upsert(externalIdField.apiName(getContext()), converted);
            for (int i = 0; i < result.length; i++) {
                UpsertResult r = result[i];
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
            SaveResult[] result = connection.update(converted);
            for (int i = 0; i < result.length; i++) {
                SaveResult r = result[i];
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

    public void delete(SObject object) throws ConnectionException {
        delete(ImmutableList.of(object), true, false);
    }

    public void delete(Collection<SObject> objects) throws ConnectionException {
        delete(objects, true, false);
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

    public void deleteById(String id) throws ConnectionException {
        deleteById(ImmutableList.of(id));
    }

    public void deleteById(Collection<String> ids) throws ConnectionException {
        deleteById(ids, true, false);
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
            DeleteResult[] res = connection.delete(portion.toArray(new String[0]));
            for (int i = 0; i < res.length; i++) {
                DeleteResult r = res[i];
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

    private static final int MIN_WAIT_TIMEOUT_MILLISECONDS = 1000;
    private static final int MAX_WAIT_TIMEOUT_MILLISECONDS = 100000000;
    private static final double METADATA_API_VERSION = 29.0;

    public <T1 extends Metadata, T2 extends Metadata> List<T2> retrieveMetadata(Class<T1> metadataClass, Class<T2> returnClass) throws ConnectionException, IOException {
        List<FileProperty> members = getItems(isFolderSpecific(metadataClass) ? getFolders(metadataClass) : new ArrayList<String>(), metadataClass);
        return deserializeMetadata(getEntries(retrieveZip(members, metadataClass), members), returnClass);
    }

    public <T extends Metadata> List<T> retrieveMetadata(Class<T> metadataClass) throws ConnectionException, IOException {
        return retrieveMetadata(metadataClass, metadataClass);
    }

    public <T extends Metadata> List<T> retrieveMetadata(Class<T> metadataClass, final String folder, String... names) throws ConnectionException, IOException {
        if (!isFolderSpecific(metadataClass)) {
            throw new UnsupportedOperationException("Requested objects must be folder specific.");
        }

        List<String> folders = new ArrayList<String>();
        folders.add(folder);
        List<FileProperty> members = getItems(folders, metadataClass, names);

        return deserializeMetadata(getEntries(retrieveZip(members, metadataClass), members), metadataClass);
    }

    public <T extends Metadata> AsyncResult[] createMetadata(List<T> objects) throws ConnectionException {
        return createMetadata(objects, 1);
    }

    public <T extends Metadata> AsyncResult[] createMetadata(List<T> objects, int timeoutMultiplier) throws ConnectionException {
        return waitResult(getMetadataConnection().create(objects.toArray(new Metadata[objects.size()])), timeoutMultiplier);
    }

    public <T extends Metadata> AsyncResult[] updateMetadata(List<T> objects) throws ConnectionException {
        return updateMetadata(objects, 1);
    }

    public <T extends Metadata> AsyncResult[] updateMetadata(List<T> objects, int timeoutMultiplier) throws ConnectionException {
        List<UpdateMetadata> updates = new ArrayList<UpdateMetadata>();
        for (Metadata object : objects) {
            UpdateMetadata updateMetadata = new UpdateMetadata();
            updateMetadata.setCurrentName(object.getFullName());
            updateMetadata.setMetadata(object);
            updates.add(updateMetadata);
        }

        return waitResult(getMetadataConnection().update(updates.toArray(new UpdateMetadata[updates.size()])), timeoutMultiplier);
    }

    public <T extends Metadata> AsyncResult[] deleteMetadata(List<T> objects) throws ConnectionException {
        return deleteMetadata(objects, 1);
    }

    public <T extends Metadata> AsyncResult[] deleteMetadata(List<T> objects, int timeoutMultiplier) throws ConnectionException {
        return waitResult(getMetadataConnection().delete(objects.toArray(new Metadata[objects.size()])), timeoutMultiplier);
    }

    public String retrievePackageVersion(Package pkg) throws ConnectionException, IOException {
        String namespace = getContext().get(pkg);
        if (Strings.isNullOrEmpty(namespace)) {
            return null;
        }
        ListMetadataQuery query = new ListMetadataQuery();
        query.setType("InstalledPackage");
        //query.setFolder(null);
        double asOfVersion = 28.0;
        // Assuming that the SOAP binding has already been established.
        FileProperties[] lmr = getMetadataConnection().listMetadata(
                new ListMetadataQuery[]{query}, asOfVersion);
        if (lmr != null) {
            for (FileProperties n : lmr) {
                if (namespace.equals(n.getNamespacePrefix())) {
                    List<InstalledPackage> installedPackages = retrieveMetadata(InstalledPackage.class);
                    for (InstalledPackage installedPackage : installedPackages) {
                        if (namespace.equals(installedPackage.getFullName())) {
                            return installedPackage.getVersionNumber();
                        }
                    }
                }
            }
        }
        return null;
    }

    private <T extends Metadata> List<T> deserializeMetadata(Map<String, byte[]> entries, Class<T> metadataClass) throws ConnectionException, IOException {
        List<T> result = new ArrayList<T>();
        FixedTypeMapper mapper = new FixedTypeMapper();
        mapper.setPackagePrefix(null);
        mapper.setConfig(getMetadataConnection().getConfig());
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(entry.getValue());
            try {
                T instance = metadataClass.newInstance();
                XmlInputStream xmlStream = new XmlInputStream();
                xmlStream.setInput(inputStream, "UTF-8");
                instance.load(xmlStream, mapper);
                instance.setFullName(entry.getKey());
                result.add(instance);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (PullParserException ex) {
                throw new RuntimeException(ex);
            } finally {
                inputStream.close();
            }
        }

        return result;
    }

    private List<String> getFolders(Class metadataClass) throws ConnectionException {
        return propertiesToNames(getFileProperties(new ArrayList<String>(), metadataClass.getSimpleName() + "Folder"));
    }

    private List<FileProperty> getItems(List<String> folders, Class metadataClass, String... objectsNames) throws ConnectionException {
        List<FileProperty> fileProperties = getFileProperties(folders, metadataClass.getSimpleName());
        if (objectsNames.length == 0) {
            return fileProperties;
        }

        List<FileProperty> result = new ArrayList<FileProperty>();
        List<String> objectsList = new ArrayList<String>(Arrays.asList(objectsNames));
        for (FileProperty fileProperty : fileProperties) {
            if (objectsList.contains(fileProperty.fullName)) {
                result.add(fileProperty);
            }
        }

        return result;
    }

    private List<FileProperty> getFileProperties(List<String> folders, String type) throws ConnectionException {
        List<ListMetadataQuery> queries = new ArrayList<ListMetadataQuery>();
        int splitBy = 100;
        if (folders.size() > 0) {
            for (String folder : folders) {
                ListMetadataQuery query = new ListMetadataQuery();
                query.setFolder(folder);
                query.setType(type);
                queries.add(query);
            }
            if ("Dashboard".equals(type)) {
                splitBy = 3;
            }
        } else {
            ListMetadataQuery query = new ListMetadataQuery();
            query.setType(type);
            queries.add(query);
        }

        List<FileProperty> result = new ArrayList<FileProperty>();
        List<FileProperties> lmr = Lists.newArrayList();
        for (List<ListMetadataQuery> queriesSublist : Lists.partition(queries, splitBy)) {
            lmr.addAll(
                    Arrays.asList(getMetadataConnection()
                            .listMetadata(
                                    queriesSublist.toArray(new ListMetadataQuery[queriesSublist.size()]),
                                    METADATA_API_VERSION
                            )
                    )
            );
        }
        for (FileProperties n : lmr) {
            result.add(new FileProperty(n.getFullName(), n.getFileName()));
        }

        return result;
    }

    private boolean isFolderSpecific(Class metadataClass) {
        return metadataClass == Document.class
                || metadataClass == Email.class
                || metadataClass == Report.class
                || metadataClass == Dashboard.class;
    }

    private <T extends Metadata> byte[] retrieveZip(List<FileProperty> membersProperties, Class<T> metadataClass) throws ConnectionException {
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        retrieveRequest.setApiVersion(METADATA_API_VERSION);
        com.sforce.soap.metadata.Package pack = new com.sforce.soap.metadata.Package();
        PackageTypeMembers members = new PackageTypeMembers();
        members.setName(metadataClass.getSimpleName());
        List<String> memberNames = new ArrayList<String>();
        for (FileProperty fileProperty : membersProperties) {
            memberNames.add(fileProperty.fullName);
        }
        members.setMembers(memberNames.toArray(new String[memberNames.size()]));
        pack.setTypes(new PackageTypeMembers[]{members});
        pack.setVersion(Double.toString(METADATA_API_VERSION));
        retrieveRequest.setUnpackaged(pack);

        AsyncResult asyncResult = waitResult(new AsyncResult[]{getMetadataConnection().retrieve(retrieveRequest)}, 1)[0];
        RetrieveResult result = getMetadataConnection().checkRetrieveStatus(asyncResult.getId());

        return result.getZipFile();
    }

    private AsyncResult[] waitResult(AsyncResult[] asyncResults, int timeoutMultiplier) throws ConnectionException {
        long wait = 0;
        AsyncResult[] processResults = asyncResults;
        for (AsyncResult result : asyncResults) {
            logger.debug("Processing AsyncResult {}", result);
        }
        while (true) {
            boolean done = true;
            List<String> ids = new ArrayList<String>();
            for (AsyncResult asyncResult : processResults) {
                if (!asyncResult.isDone()) {
                    ids.add(asyncResult.getId());
                    done = false;
                }
            }
            logger.debug("AsyncResut; Done: {}, StillInProgress: {}", done, ids);

            if (done) {
                break;
            }
            try {
                Thread.sleep(MIN_WAIT_TIMEOUT_MILLISECONDS);
            } catch (InterruptedException ex) {
            }
            processResults = getMetadataConnection().checkStatus(ids.toArray(new String[ids.size()]));
            wait += MIN_WAIT_TIMEOUT_MILLISECONDS;
            if (wait > MAX_WAIT_TIMEOUT_MILLISECONDS * timeoutMultiplier) {
                throw new RuntimeException("Metadata retrieve zip request timed out.");
            }
        }

        for (AsyncResult asyncResult : processResults) {
            logger.debug("Done AsyncResult {}", asyncResult);
            if (asyncResult.getState() != AsyncRequestState.Completed) {
                throw new RuntimeException(asyncResult.getStatusCode() + " msg: " +
                        asyncResult.getMessage());
            }
        }

        return processResults;
    }

    private Map<String, byte[]> getEntries(byte[] zip, List<FileProperty> members) throws IOException {
        final String unpackagedRelativePath = "unpackaged/";
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        ByteArrayInputStream byteStream = new ByteArrayInputStream(zip);
        ZipInputStream zipStream = new ZipInputStream(byteStream);
        try {
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().equals(unpackagedRelativePath + "package.xml")) {
                    continue;
                }

                int size;
                byte[] buffer = new byte[2048];
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(byteOutput, buffer.length);
                try {
                    while ((size = zipStream.read(buffer, 0, buffer.length)) != -1) {
                        bufferedOutput.write(buffer, 0, size);
                    }

                    bufferedOutput.flush();
                    entries.put(findPropertyByFileName(members, entry.getName().substring(unpackagedRelativePath.length())).fullName, byteOutput.toByteArray());
                } finally {
                    bufferedOutput.close();
                }
            }
        } finally {
            zipStream.close();
        }

        return entries;
    }

    private List<String> propertiesToNames(List<FileProperty> properties) {
        List<String> result = new ArrayList<String>();
        for (FileProperty property : properties) {
            result.add(property.fullName);
        }

        return result;
    }

    private FileProperty findPropertyByFileName(List<FileProperty> properties, String fileName) {
        for (FileProperty property : properties) {
            if (property.fileName.equals(fileName)) {
                return property;
            }
        }

        return null;
    }

    private class FileProperty {
        public FileProperty(String fullName, String fileName) {
            this.fullName = fullName;
            this.fileName = fileName;
        }

        public String fullName;
        public String fileName;
    }
}