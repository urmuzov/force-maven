package com.sforce.contrib.partner;

import com.google.common.collect.Lists;

import java.util.*;

public class Package {

    private Map<String, SObjectType> bySfName = new HashMap<String, SObjectType>();

    public Package() {
    }

    public String name() {
        return getClass().getName();
    }

    protected <T extends SObjectType> void add(T object) {
        if (object instanceof CustomSettings) {
            if (((CustomSettings) object).visibility() == CustomSettingsVisibility.PROTECTED) {
                return;
            }
        }

        this.bySfName.put(object.sfName(), object);
    }

    public Collection<String> allSfNames() {
        return bySfName.keySet();
    }

    public Collection<SObjectType> allSObjectTypes() {
        return bySfName.values();
    }

    public Collection<SObjectType> customSObjectTypes() {
        List<SObjectType> result = Lists.newArrayList();
        for (SObjectType objectMeta : allSObjectTypes()) {
            if (objectMeta.custom()) {
                result.add(objectMeta);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public SObjectType bySfName(String sfName) {
        return bySfName.get(sfName);
    }

}
