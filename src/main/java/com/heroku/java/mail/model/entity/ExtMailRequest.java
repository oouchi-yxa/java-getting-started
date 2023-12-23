package com.heroku.java.mail.model.entity;

import com.heroku.java.mail.model.value.ExtMailAddress;
import com.heroku.java.mail.model.value.ExtMailBody;
import com.heroku.java.mail.model.value.ExtMailSubject;
import lombok.Builder;
import lombok.Getter;

/**
 * メール送信リクエスト情報
 */
@Builder
@Getter
public class ExtMailRequest {

    /**
     * 送信先メールアドレス
     */
    private ExtMailAddress to;

    /**
     * 送信元メールアドレス
     */
    private ExtMailAddress from;

    /**
     * メール件名
     */
    private ExtMailSubject subject;

    /**
     * メール本文
     */
    private ExtMailBody body;
}
