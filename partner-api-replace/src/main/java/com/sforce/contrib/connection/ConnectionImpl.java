package com.sforce.contrib.connection;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sforce.contrib.partner.Context;
import com.sforce.contrib.partner.Field;
import com.sforce.contrib.partner.Package;
import com.sforce.contrib.partner.SObject;
import com.sforce.soap.metadata.*;
import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.soap.partner.*;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sforce.contrib.partner.Package;

/**
 * User: urmuzov
 * Date: 12/4/12
 * Time: 7:04 AM
 */
public class ConnectionImpl extends ConnectionBase {

    public static boolean check(String username, String password, String token) {
        try {
            ConnectionImpl.createNewConnection(username, password, token, new Context());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    //private final Pattern SERVICE_ENDPOINT_PATTERN = Pattern.compile("^https://([^-]+)(-api)?.salesforce.com/.*$");
    // For example:
    // https://na15.salesforce.com/services/Soap/u/26.0/00Di0000000aNfm
    // https://cloudaware-7261.cloudforce.com/services/Soap/u/26.0/00Di0000000aNfm
    // https://c.na15.visual.force.com/services/Soap/u/28.0/00Di0000000JC88
    private final Pattern SERVICE_ENDPOINT_PATTERN = Pattern.compile("^https://(c\\.)?(.+)(-api)?\\.(salesforce|visual\\.force|cloudforce)\\.com/.*$");

    private static final Logger logger = LoggerFactory.getLogger(ConnectionImpl.class);

    private MetadataConnection metadataConnection;

    private ConnectionDto connectionDto;
    private final PartnerConnection connection;

    public static Connection createNewConnection(String username, String password, String token, Context context) throws ConnectionException {
        ConnectionImpl connection = new ConnectionImpl(createConnection(username, password, token), context);
        connection.onApiRequest(ApiRequestType.LOGIN);
        return connection;
    }

    public static Connection createNewConnection(String serviceEndpoint, String sessionId, Context context) throws ConnectionException {
        return new ConnectionImpl(serviceEndpoint, sessionId, context);
    }

    public static Connection restoreConnection(ConnectionDto connectionDto) throws ConnectionException {
        return new ConnectionImpl(connectionDto);
    }

    public static Connection restoreConnection(ConnectionDto connectionDto, ConnectorConfig config) throws ConnectionException {
        return new ConnectionImpl(connectionDto, config);
    }

    protected ConnectionImpl(String serviceEndpoint, String sessionId, Context context) throws ConnectionException {
        this.connection = createConnection(serviceEndpoint, sessionId);
        this.connectionDto = createConnectionDto(this.connection, context);
    }

    protected ConnectionImpl(PartnerConnection partnerConnection, Context context) throws ConnectionException {
        this.connection = partnerConnection;
        this.connectionDto = createConnectionDto(partnerConnection, context);
    }

    protected ConnectionImpl(ConnectionDto connectionDto) throws ConnectionException {
        this.connection = createConnection(connectionDto.getServiceEndpoint(), connectionDto.getSessionId());
        this.connectionDto = connectionDto;
    }

    protected ConnectionImpl(ConnectionDto connectionDto, ConnectorConfig config) throws ConnectionException {
        this.connection = createConnection(connectionDto.getServiceEndpoint(), connectionDto.getSessionId(), config);
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
        config.setConnectionTimeout(600000);
        config.setReadTimeout(600000);

        return new PartnerConnection(config);
    }

    private static PartnerConnection createConnection(String serviceEndpoint, String sessionId) throws ConnectionException {
        ConnectorConfig config = new ConnectorConfig();
        config.setConnectionTimeout(600000);
        config.setReadTimeout(600000);
        return createConnection(serviceEndpoint, sessionId, config);
    }

    private static PartnerConnection createConnection(String serviceEndpoint, String sessionId, ConnectorConfig config) throws ConnectionException {
        if (serviceEndpoint == null || serviceEndpoint.isEmpty()) {
            throw new IllegalArgumentException("serviceEndpoint");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("sessionId");
        }

        config.setSessionId(sessionId);
        config.setServiceEndpoint(serviceEndpoint);

        return new PartnerConnection(config);
    }

    @Override
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

    @Override
    public ConnectionDto getConnectionDto() {
        return connectionDto;
    }

    @Override
    public Context getContext() {
        return connectionDto.getContext();
    }

    @Override
    public PartnerConnection getPartnerConnection() {
        return connection;
    }

    @Override
    public String getSessionId() {
        String sessionId = connection.getConfig().getSessionId();
        return sessionId.substring(sessionId.indexOf("!") + 1);
    }

    @Override
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

    @Override
    public String getSalesforceInstance() {
        String serviceEndpoint = connection.getConfig().getServiceEndpoint();
        Matcher matcher = SERVICE_ENDPOINT_PATTERN.matcher(serviceEndpoint);
        if (!matcher.matches()) {
            throw new RuntimeException("Can't find salesforce instance id in serviceEndpoint url");
        }
        return matcher.group(2);
    }

    @Override
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

    @Override
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

    @Override
    public List<Save> update(Collection<SObject> objects, boolean exceptionOnFail, boolean slowDown) throws ConnectionException {
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

    @Override
    public List<Delete> flush(String objectType, boolean exceptionOnFail) throws ConnectionException {
        List<SObject> result = query(Soql.from(objectType).select("Id"));
        List<String> ids = Lists.newArrayList();
        for (SObject o : result) {
            ids.add(o.getId());
        }
        return deleteById(ids, exceptionOnFail, false, true);
    }

    @Override
    public List<Delete> deleteById(Collection<String> ids, boolean exceptionOnFail, boolean slowDown, boolean emptyRecycleBin) throws ConnectionException {
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
            if (emptyRecycleBin) {
                EmptyRecycleBinResult[] emptyRecycleBinResults = connection.emptyRecycleBin(portion.toArray(new String[0]));
            }
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

    @Override
    public Result queryStart(Soql query) throws ConnectionException {
        String q = query.compile(getContext());
        logger.info("Quering '{}'", q);
        QueryResult result = connection.query(q);
        onApiRequest(ApiRequestType.QUERY);
        List<SObject> out = Lists.newArrayList();
        for (com.sforce.soap.partner.sobject.SObject oIn : result.getRecords()) {
            if (query.getFromType() == null) {
                out.add(new SObject(oIn));
            } else {
                out.add(new SObject(oIn, query.getFromType()));
            }
        }
        return new Result(result.getDone(), result.getQueryLocator(), out, query.getFromType(), query.getFromString());
    }

    @Override
    public Result queryMore(Result previousResult) throws ConnectionException {
        logger.info("Quering more {}", previousResult.queryLocator);
        QueryResult result = connection.queryMore(previousResult.queryLocator);
        onApiRequest(ApiRequestType.QUERY);
        List<SObject> out = Lists.newArrayList();
        for (com.sforce.soap.partner.sobject.SObject oIn : result.getRecords()) {
            if (previousResult.type == null) {
                out.add(new SObject(oIn));
            } else {
                out.add(new SObject(oIn, previousResult.type));
            }
        }
        return new Result(result.getDone(), result.getQueryLocator(), out, previousResult.type, previousResult.typeString);
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

    @Override
    public Integer getApiRequestLimit() {
        LimitInfo info = getApiRequestLimitInfo();
        return info == null ? null : info.getLimit();
    }

    @Override
    public Integer getApiRequestCount() {
        LimitInfo info = getApiRequestLimitInfo();
        return info == null ? null : info.getCurrent();
    }


    /**
     * METADATA
     */
    private static final int METADATA_BATCH_SIZE = 10;

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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