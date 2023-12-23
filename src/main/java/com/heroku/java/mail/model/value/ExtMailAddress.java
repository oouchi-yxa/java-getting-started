package com.heroku.java.mail.model.value;

import lombok.Value;
import org.apache.commons.lang3.StringUtils;

/**
 * 送信先メールアドレス
 */
@Value
public class ExtMailAddress {

    /**
     * 値
     */
    String value;

    public ExtMailAddress(final String value) {

        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("mail address value is empty.");
        }

        this.value = value;
    }
}
