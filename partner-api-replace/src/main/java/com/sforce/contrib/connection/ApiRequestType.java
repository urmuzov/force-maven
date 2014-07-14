package com.sforce.contrib.connection;

/**
 * User: urmuzov
 * Date: 11.07.14
 * Time: 17:20
 */
public enum ApiRequestType {
    LOGIN,
    APEX_REST,
    DATA,
    CREATE,
    UPSERT,
    UPDATE,
    DELETE,
    QUERY,
    METADATA_CREATE,
    METADATA_UPDATE,
    METADATA_DELETE,
    METADATA_LIST,
    METADATA_RETRIEVE,
    METADATA_CHECK_RETRIEVE_STATUS,
    METADATA_CHECK_STATUS,
}
