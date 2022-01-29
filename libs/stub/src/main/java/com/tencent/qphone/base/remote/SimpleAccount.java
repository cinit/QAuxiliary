package com.tencent.qphone.base.remote;

import java.util.HashMap;

public class SimpleAccount {

    public static final String _ISLOGINED = "_isLogined";
    public static final String _LOGINPROCESS = "_loginedProcess";
    public static final String _LOGINTIME = "_loginTime";
    public static final String _UIN = "_uin";
    private static final String tag = "SimpleAccount";
    private HashMap attributes = new HashMap();

    public static SimpleAccount parseSimpleAccount(String src) {
        throw new RuntimeException("Stub!");
    }

    public static boolean isSameAccount(SimpleAccount A, SimpleAccount B) {
        return A.getUin().equals(B.getUin()) && A.isLogined() == B.isLogined();
    }

    public String getUin() {
        return getAttribute(_UIN, "");
    }

    public void setUin(String uin) {
        setAttribute(_UIN, uin);
    }

    public boolean isLogined() {
        return Boolean.parseBoolean(getAttribute(_ISLOGINED, String.valueOf(false)));
    }

    public String getLoginProcess() {
        return getAttribute(_LOGINPROCESS, "");
    }

    public void setLoginProcess(String loginProcess) {
        setAttribute(_LOGINPROCESS, loginProcess);
    }

    public boolean containsKey(String key) {
        return this.attributes.containsKey(key);
    }

    public String getAttribute(String key, String defaultValue) {
        if (this.attributes.containsKey(key)) {
            return (String) this.attributes.get(key);
        }
        return defaultValue;
    }

    public String removeAttribute(String key) {
        return (String) this.attributes.remove(key);
    }

    public void setAttribute(String key, String value) {
        if (key.indexOf(" ") > 0) {
            throw new RuntimeException("key found space ");
        }
        this.attributes.put(key, value);
    }

    public String toString() {
        throw new RuntimeException("Stub!");
    }

    public String toStoreString() {
        throw new RuntimeException("Stub!");
    }

    public HashMap getAttributes() {
        return this.attributes;
    }

    public boolean equals(Object o) {
        if (o instanceof SimpleAccount) {
            return isSameAccount(this, (SimpleAccount) o);
        }
        return false;
    }
}
