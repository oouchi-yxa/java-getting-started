package com.heroku.java.mail.model.value;

import lombok.Value;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * メール件名
 */
@Value
public class ExtMailSubject {
    /** メッセージ文字列 */
    String value;
}
