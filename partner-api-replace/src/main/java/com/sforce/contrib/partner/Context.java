package com.sforce.contrib.partner;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * User: urmuzov
 * Date: 17.04.14
 * Time: 18:53
 */
public class Context {
    public static String withNamespace(String namespace, String name) {
        return Strings.isNullOrEmpty(namespace) ? name : namespace + "__" + name;
    }

    public static String withoutNamespace(String namespace, String name) {
        if (name == null) {
            return null;
        }
        if (namespace == null) {
            return name;
        }
        String ns = namespace.toLowerCase() + "__";
        if (name.toLowerCase().startsWith(ns)) {
            return name.substring(ns.length());
        }
        return name;
    }

    private Map<String, String> pkgToNamespace = Maps.newHashMap();

    public Context put(Package pkg, String namespace) {
        pkgToNamespace.put(pkg.name(), namespace);
        return this;
    }

    public String withNamespace(Package pkg, String name) {
        return withNamespace(get(pkg), name);
    }

    public String withoutNamespace(Package pkg, String name) {
        return withoutNamespace(get(pkg), name);
    }

    public String get(Package pkg) {
        return pkgToNamespace.get(pkg.name());
    }

    public Map<String, String> getPkgToNamespace() {
        return pkgToNamespace;
    }

    public void setPkgToNamespace(Map<String, String> pkgToNamespace) {
        this.pkgToNamespace = pkgToNamespace;
    }
}
