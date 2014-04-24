package com.sforce.contrib.metadata;

import com.sforce.soap.metadata.Picklist;

public class FixedPicklist extends Picklist {

    private static final com.sforce.ws.bind.TypeInfo controllingField__typeInfo =
            new com.sforce.ws.bind.TypeInfo("http://soap.sforce.com/2006/04/metadata","controllingField","http://www.w3.org/2001/XMLSchema","string",0,1,true);
    private static final com.sforce.ws.bind.TypeInfo picklistValues__typeInfo =
            new com.sforce.ws.bind.TypeInfo("http://soap.sforce.com/2006/04/metadata","picklistValues","http://soap.sforce.com/2006/04/metadata","PicklistValue",0,-1,true);
    private static final com.sforce.ws.bind.TypeInfo sorted__typeInfo =
            new com.sforce.ws.bind.TypeInfo("http://soap.sforce.com/2006/04/metadata","sorted","http://www.w3.org/2001/XMLSchema","boolean",1,1,true);
    private static final com.sforce.ws.bind.TypeInfo restrictedPicklist__typeInfo =
            new com.sforce.ws.bind.TypeInfo("http://soap.sforce.com/2006/04/metadata","restrictedPicklist","http://www.w3.org/2001/XMLSchema","boolean",0,1,true);

    private boolean restrictedPicklist;
    private boolean restrictedPicklist__is_set;

    @Override
    public void load(com.sforce.ws.parser.XmlInputStream __in,
                     com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
        __typeMapper.consumeStartTag(__in);
        loadFields(__in, __typeMapper);
        __typeMapper.consumeEndTag(__in);
    }

    public void setRestrictedPicklist(boolean restrictedPicklist) {
        this.restrictedPicklist = restrictedPicklist;
        restrictedPicklist__is_set = true;
    }

    protected void loadFields(com.sforce.ws.parser.XmlInputStream __in,
                              com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
        __in.peekTag();
        if (__typeMapper.isElement(__in, controllingField__typeInfo)) {
            setControllingField(__typeMapper.readString(__in, controllingField__typeInfo, String.class));
        }
        __in.peekTag();
        if (__typeMapper.isElement(__in, picklistValues__typeInfo)) {
            setPicklistValues((com.sforce.soap.metadata.PicklistValue[])__typeMapper.readObject(__in, picklistValues__typeInfo, com.sforce.soap.metadata.PicklistValue[].class));
        }
        __in.peekTag();
        if (__typeMapper.isElement(__in, restrictedPicklist__typeInfo)) {
            setRestrictedPicklist(__typeMapper.readBoolean(__in, restrictedPicklist__typeInfo, boolean.class));
        }
        __in.peekTag();
        if (__typeMapper.verifyElement(__in, sorted__typeInfo)) {
            setSorted(__typeMapper.readBoolean(__in, sorted__typeInfo, boolean.class));
        }
    }
}
