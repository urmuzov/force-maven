package com.sforce.contrib.partner;

public class User extends SObjectType {

    public void init(Package pkg) {
        super.init(pkg, "User", "User", "Users");
    }
}
