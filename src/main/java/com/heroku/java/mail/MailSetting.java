package com.heroku.java.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailSetting {

    String url = System.getenv("MAIL_URL");

    String key = System.getenv("MAIL_KEY");

}
