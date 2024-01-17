package com.heroku.java.cms;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CmsSetting {

    String access_key_id = System.getenv("CLOUDCUBE_ACCESS_KEY_ID");

    String secret_access_key = System.getenv("CLOUDCUBE_SECRET_ACCESS_KEY");

    String url = System.getenv("CLOUDCUBE_URL");

    String bucket = "";

    String basePrefix = "";
}
