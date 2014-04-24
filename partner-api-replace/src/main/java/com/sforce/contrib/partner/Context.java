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

    private Map<String, String> pkgToNamespace = Maps.newHashMap();

    public Context put(Package pkg, String namespace) {
        pkgToNamespace.put(pkg.name(), namespace);
        return this;
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
