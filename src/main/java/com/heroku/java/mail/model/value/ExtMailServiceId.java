package com.heroku.java.mail.model.value;

import lombok.Value;

/**
 * サービス識別子
 */
@Value
public class ExtMailServiceId {

    public static final ExtMailServiceId STUB;
    public static final ExtMailServiceId AUTOBAHN;

    static {
        STUB = new ExtMailServiceId("STUB");
        AUTOBAHN = new ExtMailServiceId("AUTOBAHN");
    }

    /**
     * サービス識別子の値
     */
    String value;
}
