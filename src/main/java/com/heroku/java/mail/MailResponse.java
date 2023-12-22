package com.heroku.java.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailResponse {
    String code;
    String error;
    String validation_errors;
}
